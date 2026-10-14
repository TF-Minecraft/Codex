package net.tfminecraft.codex.character;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import net.tfminecraft.codex.managers.PlayerDataManager;
import net.tfminecraft.codex.model.data.PlayerData;
import net.tfminecraft.tlibs.character.CharacterInfo;
import net.tfminecraft.tlibs.character.CharacterRef;
import net.tfminecraft.tlibs.character.CharacterSessionEndEvent;
import net.tfminecraft.tlibs.character.CharacterSessionStartEvent;
import net.tfminecraft.tlibs.character.Characters;

/**
 * Gives each character its own journal: loaded when a character session starts and saved when it
 * ends. Between characters a player has no journal, so nothing can be discovered. Alongside it sits
 * what the account's other characters know, so entries stay readable out of character. Main thread only.
 */
public final class CharacterJournals implements Listener {
    private final PlayerDataManager players;
    private final CharacterStore store;
    private final Logger logger;
    private final Map<UUID, AccountKnowledge> knowledge = new HashMap<>();
    private BukkitTask sweep;

    public CharacterJournals(PlayerDataManager players, CharacterStore store, Logger logger) {
        this.players = players;
        this.store = store;
        this.logger = logger;
    }

    /** Opens journals for characters already in play and moves old account files once heirs are known. */
    public void start(Plugin plugin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Characters.currentSession(player).ifPresent(session -> open(player, session.ref()));
        }
        sweep = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (Characters.legacyHeirsReady()) {
                sweep.cancel();
                migrateAll();
            }
        }, 1L, 20L);
    }

    public void stop() {
        if (sweep != null) {
            sweep.cancel();
        }
        saveAll();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onStart(CharacterSessionStartEvent event) {
        open(event.getPlayer(), event.getSession().ref());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnd(CharacterSessionEndEvent event) {
        close(event.getPlayer(), event.getSession().ref().characterId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        close(event.getPlayer(), null);
    }

    void open(Player player, CharacterRef ref) {
        close(player, null);
        migrate(ref.ownerId());
        try {
            players.addPlayer(store.load(ref.ownerId(), ref.characterId(), player.getName()));
            knowledge.put(ref.ownerId(), knowledgeOf(ref, player.getName()));
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Codex for character " + ref.characterId() + " of " + player.getName()
                    + " could not be read; nothing can be discovered until it is fixed", ex);
        }
    }

    /** Saves and unloads the player's journal; with a character ID, only that character's. */
    void close(Player player, UUID characterId) {
        PlayerData data = players.getPlayerByUUID(player.getUniqueId());
        if (data == null || characterId != null && !characterId.equals(data.getCharacterId())) {
            return;
        }
        save(data);
        players.removePlayer(data);
        knowledge.remove(player.getUniqueId());
    }

    /** What the player's other characters know, or null between characters. */
    public AccountKnowledge knowledge(Player player) {
        return knowledge.get(player.getUniqueId());
    }

    /** Rereads the other characters' journals, for example after staff reset them. */
    public void refreshKnowledge() {
        for (PlayerData data : players.getPlayers().values()) {
            knowledge.put(data.getUuid(), knowledgeOf(new CharacterRef(data.getUuid(), data.getCharacterId()), data.getName()));
        }
    }

    private AccountKnowledge knowledgeOf(CharacterRef ref, String fallbackName) {
        String name = fallbackName;
        AccountKnowledge.Builder builder = new AccountKnowledge.Builder();
        for (CharacterInfo info : Characters.characters(ref.ownerId()).orElse(List.of())) {
            UUID id = info.ref().characterId();
            if (id.equals(ref.characterId())) {
                name = info.name();
            } else if (Files.exists(store.characterFile(id))) {
                try {
                    builder.add(store.load(ref.ownerId(), id, info.name()).getCategories(), info.name(), info.alive());
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "Codex for character " + id + " could not be read; "
                            + "its discoveries are left out of its account's reference", ex);
                }
            }
        }
        return builder.build(name);
    }

    public void saveAll() {
        for (PlayerData data : new ArrayList<>(players.getPlayers().values())) {
            save(data);
        }
    }

    private void save(PlayerData data) {
        if (!data.isModified()) {
            return;
        }
        try {
            store.save(data);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not save Codex for character " + data.getCharacterId(), ex);
        }
    }

    void migrateAll() {
        List<UUID> owners;
        try {
            owners = store.legacyOwners();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not list pre-character Codex files", ex);
            return;
        }
        int moved = 0;
        for (UUID owner : owners) {
            if (migrate(owner)) {
                moved++;
            }
        }
        if (!owners.isEmpty()) {
            logger.info("Moved " + moved + " of " + owners.size() + " account Codex files to characters; "
                    + "the rest move when their account has a character");
        }
    }

    /** Merges the account's old discoveries into its heir, then sets the old file aside. */
    boolean migrate(UUID owner) {
        if (!store.hasLegacy(owner)) {
            return false;
        }
        Optional<UUID> heir = Heirs.of(owner);
        if (heir.isEmpty()) {
            return false;
        }
        try {
            CharacterStore.LegacyJournal legacy = store.readLegacy(owner);
            PlayerData target = players.getPlayerByUUID(owner);
            if (target == null || !heir.get().equals(target.getCharacterId())) {
                target = store.load(owner, heir.get(), legacy.name());
            }
            int added = target.absorb(legacy.categories());
            store.save(target);
            store.retireLegacy(owner);
            logger.info("Moved " + added + " Codex discoveries of account " + owner + " to character " + heir.get());
            return true;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not move the Codex of account " + owner + " to its heir", ex);
            return false;
        }
    }
}
