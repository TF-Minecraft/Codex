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
 * what the account knows from its other characters and from before journals were per character, so
 * entries stay readable out of character. Main thread only.
 */
public final class CharacterJournals implements Listener {
    private final PlayerDataManager players;
    private final CharacterStore store;
    private final Logger logger;
    private final Map<UUID, AccountKnowledge> knowledge = new HashMap<>();
    /** Journals of characters no longer in play whose last save failed, by character; retried with every save. */
    private final Map<UUID, PlayerData> unsaved = new HashMap<>();

    public CharacterJournals(PlayerDataManager players, CharacterStore store, Logger logger) {
        this.players = players;
        this.store = store;
        this.logger = logger;
    }

    /** Opens journals for characters already in play, for example when the plugin enables late. */
    public void start() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Characters.currentSession(player).ifPresent(session -> open(player, session.ref()));
        }
    }

    public void stop() {
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
        seed(ref.ownerId());
        try {
            PlayerData pending = unsaved.remove(ref.characterId());
            players.addPlayer(pending != null ? pending : store.load(ref.ownerId(), ref.characterId(), player.getName()));
            knowledge.put(ref.ownerId(), knowledgeOf(ref, player.getName()));
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Codex for character " + ref.characterId() + " of " + player.getName()
                    + " could not be read; nothing can be discovered until it is fixed", ex);
        }
    }

    /**
     * Once per account: copies its journal from before characters had their own into its most-played
     * living character's journal, chosen at the account's first session after the change. The old
     * file stays as out-of-character reference and records the copy, so it never happens twice.
     */
    void seed(UUID owner) {
        if (!store.hasLegacy(owner)) {
            return;
        }
        try {
            CharacterStore.LegacyJournal legacy = store.readLegacy(owner);
            if (legacy.seededTo() != null) {
                return;
            }
            Optional<UUID> heir = MostPlayed.of(Characters.characters(owner));
            if (heir.isEmpty()) {
                return;
            }
            PlayerData target = unsaved.containsKey(heir.get())
                    ? unsaved.get(heir.get())
                    : store.load(owner, heir.get(), legacy.name());
            int added = target.absorb(legacy.categories());
            store.save(target);
            unsaved.remove(heir.get());
            store.markSeeded(owner, heir.get());
            logger.info("Copied " + added + " old Codex discoveries of account " + owner
                    + " to its most-played character " + heir.get());
        } catch (IOException | RuntimeException ex) {
            logger.log(Level.SEVERE, "Could not copy the old Codex of account " + owner
                    + " to its most-played character; trying again at its next session", ex);
        }
    }

    /** Saves and unloads the player's journal; with a character ID, only that character's. */
    void close(Player player, UUID characterId) {
        PlayerData data = players.getPlayerByUUID(player.getUniqueId());
        if (data == null || characterId != null && !characterId.equals(data.getCharacterId())) {
            return;
        }
        if (!save(data)) {
            unsaved.put(data.getCharacterId(), data);
        }
        players.removePlayer(data);
        knowledge.remove(player.getUniqueId());
    }

    /** Journals of characters out of play that still wait to be saved; staff resets apply to them too. */
    public List<PlayerData> unsavedJournals() {
        return List.copyOf(unsaved.values());
    }

    /** What the player's account knows beyond the played character, or null between characters. */
    public AccountKnowledge knowledge(Player player) {
        return knowledge.get(player.getUniqueId());
    }

    /** Rereads the other journals, for example after staff reset them. */
    public void refreshKnowledge() {
        for (PlayerData data : players.getPlayers().values()) {
            knowledge.put(data.getUuid(), knowledgeOf(new CharacterRef(data.getUuid(), data.getCharacterId()), data.getName()));
        }
    }

    public void saveAll() {
        for (PlayerData data : new ArrayList<>(players.getPlayers().values())) {
            save(data);
        }
        unsaved.values().removeIf(this::save);
    }

    /** Whether the journal is on disk; a failed save leaves it modified. */
    private boolean save(PlayerData data) {
        if (!data.isModified()) {
            return true;
        }
        try {
            store.save(data);
            return true;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not save Codex for character " + data.getCharacterId()
                    + "; keeping it in memory and retrying with the next save", ex);
            return false;
        }
    }

    private AccountKnowledge knowledgeOf(CharacterRef ref, String fallbackName) {
        try {
            return readKnowledge(ref, fallbackName);
        } catch (RuntimeException ex) {
            logger.log(Level.WARNING, "Could not read the other characters of account " + ref.ownerId()
                    + "; only this character's own Codex is shown", ex);
            return AccountKnowledge.empty(fallbackName);
        }
    }

    private AccountKnowledge readKnowledge(CharacterRef ref, String fallbackName) {
        String name = fallbackName;
        AccountKnowledge.Builder builder = new AccountKnowledge.Builder();
        // What the account found before journals were per character: readable, but no character knows it.
        if (store.hasLegacy(ref.ownerId())) {
            try {
                builder.add(store.readLegacy(ref.ownerId()).categories(), null, false);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "The account Codex of " + ref.ownerId() + " could not be read; "
                        + "its old discoveries are left out of its reference", ex);
            }
        }
        List<CharacterInfo> roster = Characters.characters(ref.ownerId()).orElse(List.of());
        for (CharacterInfo info : roster) {
            UUID id = info.ref().characterId();
            if (id.equals(ref.characterId())) {
                name = info.name();
            } else if (unsaved.containsKey(id)) {
                builder.add(unsaved.get(id).getCategories(), info.name(), info.alive());
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
}
