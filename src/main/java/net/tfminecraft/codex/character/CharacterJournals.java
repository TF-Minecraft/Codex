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
import net.tfminecraft.codex.model.data.PlayerDataCategory;
import net.tfminecraft.tlibs.character.CharacterInfo;
import net.tfminecraft.tlibs.character.CharacterRef;
import net.tfminecraft.tlibs.character.CharacterSessionEndEvent;
import net.tfminecraft.tlibs.character.CharacterSessionStartEvent;
import net.tfminecraft.tlibs.character.Characters;

/**
 * Gives each character its own journal: loaded when a character session starts and saved when it
 * ends. Between characters a player has no journal, so nothing can be discovered. Alongside it sits
 * what the account knows from its other characters and from before journals were per character, so
 * entries stay readable out of character. Shared discoveries (see {@link SharedExploration}) are copied
 * into the journal shortly after its session starts. Main thread only.
 */
public final class CharacterJournals implements Listener {
    /** Shared discoveries arrive after the character's class and body are in place, and once the screen settles. */
    static final long SHARE_DELAY_TICKS = 40L;

    private final PlayerDataManager players;
    private final CharacterStore store;
    private final Logger logger;
    private final LegacyRecipient recipients;
    private final Exploration exploration;
    private final Map<UUID, AccountKnowledge> knowledge = new HashMap<>();
    /** Journals of characters no longer in play whose last save failed, by character; retried with every save. */
    private final Map<UUID, PlayerData> unsaved = new HashMap<>();

    /** An account's journal other than the played character's; {@code name} is null for the one from before characters. */
    private record Journal(List<PlayerDataCategory> categories, String name, boolean alive) {
    }

    public CharacterJournals(PlayerDataManager players, CharacterStore store, LegacyRecipient recipients,
            Exploration exploration, Logger logger) {
        this.players = players;
        this.store = store;
        this.recipients = recipients;
        this.exploration = exploration;
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
            if (exploration.settings().enabled()) {
                exploration.later(SHARE_DELAY_TICKS, () -> share(player, ref));
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Codex for character " + ref.characterId() + " of " + player.getName()
                    + " could not be read; nothing can be discovered until it is fixed", ex);
        }
    }

    /**
     * Once per account: copies its journal from before characters had their own into its most-played
     * living character's journal, chosen at the account's first session after the change and pinned
     * for good (see {@link LegacyRecipient}). The old file stays as out-of-character reference and
     * records the copy; a retry after a failure goes to the same character and adds nothing twice.
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
            Optional<UUID> heir = recipients.of(owner, () -> MostPlayed.of(Characters.characters(owner)));
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

    /**
     * Copies into the played character's journal the shared discoveries its account found elsewhere,
     * then runs their rewards that carry over. Only while that character is still in play. The journal
     * is saved before the rewards run, so a crash can lose a reward but never pay it twice; a failed save
     * keeps the journal in memory, saved with the next save.
     */
    void share(Player player, CharacterRef ref) {
        PlayerData journal = players.getPlayerByUUID(ref.ownerId());
        if (!player.isOnline() || journal == null || !ref.characterId().equals(journal.getCharacterId())) {
            return;
        }
        List<List<PlayerDataCategory>> others = new ArrayList<>();
        for (Journal other : otherJournals(ref)) {
            others.add(other.categories());
        }
        List<SharedExploration.Copy> copies = SharedExploration.copy(journal, others, exploration.settings());
        if (copies.isEmpty()) {
            return;
        }
        save(journal);
        logger.info("Shared " + copies.size() + " Codex discoveries of account " + ref.ownerId()
                + " with its character " + ref.characterId());
        exploration.reward(player, copies);
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
        // What the account found before journals were per character is readable, but no character knows it.
        for (Journal other : otherJournals(ref)) {
            builder.add(other.categories(), other.name(), other.alive());
        }
        for (CharacterInfo info : Characters.characters(ref.ownerId()).orElse(List.of())) {
            if (info.ref().characterId().equals(ref.characterId())) {
                name = info.name();
            }
        }
        return builder.build(name);
    }

    /** The account's journal from before characters had their own, then each other character's, alive or dead. */
    private List<Journal> otherJournals(CharacterRef ref) {
        List<Journal> journals = new ArrayList<>();
        if (store.hasLegacy(ref.ownerId())) {
            try {
                journals.add(new Journal(store.readLegacy(ref.ownerId()).categories(), null, false));
            } catch (IOException ex) {
                logger.log(Level.WARNING, "The account Codex of " + ref.ownerId() + " could not be read; "
                        + "its old discoveries are left out of its reference", ex);
            }
        }
        for (CharacterInfo info : Characters.characters(ref.ownerId()).orElse(List.of())) {
            UUID id = info.ref().characterId();
            if (id.equals(ref.characterId())) {
                continue;
            }
            if (unsaved.containsKey(id)) {
                journals.add(new Journal(unsaved.get(id).getCategories(), info.name(), info.alive()));
            } else if (Files.exists(store.characterFile(id))) {
                try {
                    journals.add(new Journal(store.load(ref.ownerId(), id, info.name()).getCategories(), info.name(),
                            info.alive()));
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "Codex for character " + id + " could not be read; "
                            + "its discoveries are left out of its account's reference", ex);
                }
            }
        }
        return journals;
    }
}
