package net.tfminecraft.codex.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import net.tfminecraft.codex.Codex;
import net.tfminecraft.codex.managers.PlayerDataManager;
import net.tfminecraft.codex.model.data.PlayerData;
import net.tfminecraft.tlibs.character.CharacterInfo;
import net.tfminecraft.tlibs.character.CharacterRef;
import net.tfminecraft.tlibs.character.CharacterSession;
import net.tfminecraft.tlibs.character.CharacterSessionEndEvent;
import net.tfminecraft.tlibs.character.CharacterSessionStartEvent;
import net.tfminecraft.tlibs.character.CharacterStatus;
import net.tfminecraft.tlibs.character.Characters;

class CharacterJournalsTest {
    @TempDir
    Path folder;

    private final UUID owner = UUID.randomUUID();
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();
    private final List<LogRecord> logs = new ArrayList<>();
    private MockedStatic<Characters> characters;
    private PlayerDataManager players;
    private CharacterStore store;
    private CharacterJournals journals;
    private Player player;

    @BeforeEach
    void setUp() {
        characters = mockStatic(Characters.class);
        characters.when(Characters::legacyHeirsReady).thenReturn(true);
        characters.when(() -> Characters.legacyHeir(owner)).thenReturn(Optional.of(new CharacterRef(owner, first)));
        players = new PlayerDataManager(mock(Codex.class));
        store = new CharacterStore(folder);
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logs.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        journals = new CharacterJournals(players, store, logger);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(owner);
        when(player.getName()).thenReturn("Steve");
    }

    @AfterEach
    void tearDown() {
        characters.close();
    }

    @Test
    void startingACharacterOpensItsOwnJournal() throws IOException {
        saved(second, "aurea");

        journals.onStart(start(second));

        PlayerData data = players.getPlayer(player);
        assertEquals(second, data.getCharacterId());
        assertEquals("Steve", data.getName());
        assertTrue(data.hasDiscovery("poi", "aurea"));
    }

    @Test
    void switchingSavesTheOutgoingJournalFirst() throws IOException {
        journals.onStart(start(first));
        players.getPlayer(player).addDiscovery("poi", "vard", "14/10/2026");

        journals.onStart(start(second));

        assertEquals(second, players.getPlayer(player).getCharacterId());
        assertFalse(players.getPlayer(player).hasDiscovery("poi", "vard"));
        assertTrue(store.load(owner, first, "Steve").hasDiscovery("poi", "vard"));
    }

    @Test
    void theHeirInheritsTheAccountJournalOnItsFirstSession() throws IOException {
        legacy("syrindell");

        journals.onStart(start(first));

        assertTrue(players.getPlayer(player).hasDiscovery("poi", "syrindell"));
        assertFalse(store.hasLegacy(owner));
        assertTrue(Files.exists(folder.resolve("players-migrated").resolve(owner + ".yml")));
    }

    @Test
    void otherCharactersStartWithoutTheAccountJournal() throws IOException {
        legacy("syrindell");

        journals.onStart(start(second));

        assertFalse(players.getPlayer(player).hasDiscovery("poi", "syrindell"));
        assertTrue(store.load(owner, first, "Steve").hasDiscovery("poi", "syrindell"));
        assertTrue(Files.readString(store.characterFile(first)).contains("name: Steve"));
    }

    @Test
    void anHeirAlreadyPlayingGetsTheDiscoveriesInMemory() throws IOException {
        characters.when(Characters::legacyHeirsReady).thenReturn(false);
        journals.onStart(start(first));
        PlayerData live = players.getPlayer(player);
        live.addDiscovery("poi", "vard", "14/10/2026");
        legacy("syrindell");
        characters.when(Characters::legacyHeirsReady).thenReturn(true);

        assertTrue(journals.migrate(owner));

        assertSame(live, players.getPlayer(player));
        assertTrue(live.hasDiscovery("poi", "syrindell"));
        assertTrue(live.hasDiscovery("poi", "vard"));
        assertTrue(store.load(owner, first, "Steve").hasDiscovery("poi", "vard"));
    }

    @Test
    void nothingMovesBeforeHeirsAreKnownOrWithoutAnOldFile() throws IOException {
        assertFalse(journals.migrate(owner));

        legacy("syrindell");
        characters.when(Characters::legacyHeirsReady).thenReturn(false);
        journals.onStart(start(first));

        assertFalse(players.getPlayer(player).hasDiscovery("poi", "syrindell"));
        assertTrue(store.hasLegacy(owner));
    }

    @Test
    void anUnreadableOldFileStaysForStaff() throws IOException {
        Files.createDirectories(folder.resolve("players"));
        Files.writeString(folder.resolve("players").resolve(owner + ".yml"), "name: [unclosed");

        assertFalse(journals.migrate(owner));

        assertTrue(store.hasLegacy(owner));
        assertLogged(Level.SEVERE, "Could not move the Codex of account " + owner);
    }

    @Test
    void anUnreadableJournalStaysClosed() throws IOException {
        Files.createDirectories(folder.resolve("characters"));
        Files.writeString(store.characterFile(first), "categories: [unclosed");

        journals.onStart(start(first));

        assertNull(players.getPlayer(player));
        assertLogged(Level.SEVERE, "could not be read");
    }

    @Test
    void endingASessionSavesOnlyThatCharacter() throws IOException {
        journals.onStart(start(first));
        players.getPlayer(player).addDiscovery("poi", "vard", "14/10/2026");

        journals.onEnd(end(second));
        assertNotNull(players.getPlayer(player));

        journals.onEnd(end(first));
        assertNull(players.getPlayer(player));
        assertTrue(store.load(owner, first, "Steve").hasDiscovery("poi", "vard"));
    }

    @Test
    void quittingSavesAndClosesAnyJournal() throws IOException {
        journals.onQuit(new PlayerQuitEvent(player, "bye"));
        journals.onStart(start(first));

        journals.onQuit(new PlayerQuitEvent(player, "bye"));

        assertNull(players.getPlayer(player));
        assertTrue(Files.exists(store.characterFile(first)));
    }

    @Test
    void unchangedJournalsAreNotRewritten() throws IOException {
        saved(first, "aurea");
        journals.onStart(start(first));
        Files.delete(store.characterFile(first));

        journals.saveAll();

        assertFalse(Files.exists(store.characterFile(first)));
    }

    @Test
    void aFailedSaveIsLogged() throws IOException {
        journals.onStart(start(first));
        Files.writeString(folder.resolve("characters"), "not a folder");

        journals.saveAll();

        assertLogged(Level.SEVERE, "Could not save Codex for character " + first);
    }

    @Test
    void startOpensPlayedCharactersThenMovesOldFilesWhenHeirsAreKnown() throws IOException {
        legacy("syrindell");
        UUID other = UUID.randomUUID();
        Files.writeString(folder.resolve("players").resolve(other + ".yml"), "name: Alex\n");
        characters.when(() -> Characters.characters(other)).thenReturn(Optional.of(List.of()));
        characters.when(() -> Characters.currentSession(player))
                .thenReturn(Optional.of(new CharacterSession(new CharacterRef(owner, second), 1)));
        characters.when(Characters::legacyHeirsReady).thenReturn(false);
        Plugin plugin = mock(Plugin.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        ArgumentCaptor<Runnable> tick = ArgumentCaptor.forClass(Runnable.class);
        when(scheduler.runTaskTimer(eq(plugin), tick.capture(), anyLong(), anyLong())).thenReturn(task);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(player));
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            journals.start(plugin);
            assertEquals(second, players.getPlayer(player).getCharacterId());

            tick.getValue().run();
            verify(task, never()).cancel();
            assertTrue(store.hasLegacy(owner));

            characters.when(Characters::legacyHeirsReady).thenReturn(true);
            tick.getValue().run();
        }

        verify(task).cancel();
        assertFalse(store.hasLegacy(owner));
        assertTrue(store.hasLegacy(other));
        assertLogged(Level.INFO, "Moved 1 of 2 account Codex files");

        journals.stop();
        assertTrue(Files.exists(store.characterFile(second)));
    }

    @Test
    void stopWithoutStartStillSaves() {
        journals.onStart(start(first));

        journals.stop();

        assertTrue(Files.exists(store.characterFile(first)));
    }

    @Test
    void anEmptyOrUnlistableOldFolderIsHandled() throws IOException {
        journals.migrateAll();
        assertTrue(logs.isEmpty());

        Path old = Files.createDirectories(folder.resolve("players"));
        assertTrue(old.toFile().setReadable(false));
        try {
            journals.migrateAll();
        } finally {
            old.toFile().setReadable(true);
        }
        assertLogged(Level.SEVERE, "Could not list pre-character Codex files");
    }

    @Test
    void otherCharactersDiscoveriesStayReadable() throws IOException {
        UUID dead = UUID.randomUUID();
        UUID unplayed = UUID.randomUUID();
        UUID broken = UUID.randomUUID();
        saved(second, "aurea");
        saved(dead, "vard");
        Files.writeString(store.characterFile(broken), "categories: [unclosed");
        characters.when(() -> Characters.characters(owner)).thenReturn(Optional.of(List.of(
                info(first, "Aldric", CharacterStatus.ALIVE), info(second, "Mira", CharacterStatus.ALIVE),
                info(dead, "Old Tom", CharacterStatus.DEAD), info(unplayed, "Hazel", CharacterStatus.ALIVE),
                info(broken, "Bram", CharacterStatus.ALIVE))));

        journals.onStart(start(first));

        AccountKnowledge knowledge = journals.knowledge(player);
        assertEquals("Aldric", knowledge.characterName());
        assertEquals(List.of("Mira"), knowledge.other("poi", "aurea").livingNames());
        assertEquals(List.of(), knowledge.other("poi", "vard").livingNames());
        assertLogged(Level.WARNING, "Codex for character " + broken + " could not be read");

        journals.onEnd(end(first));
        assertNull(journals.knowledge(player));
    }

    @Test
    void anUnknownRosterLeavesOnlyThePlayedCharacter() {
        characters.when(() -> Characters.characters(owner)).thenReturn(Optional.empty());

        journals.onStart(start(first));

        assertEquals("Steve", journals.knowledge(player).characterName());
    }

    @Test
    void refreshingRereadsOtherCharacters() throws IOException {
        characters.when(() -> Characters.characters(owner)).thenReturn(Optional.of(List.of(
                info(first, "Aldric", CharacterStatus.ALIVE), info(second, "Mira", CharacterStatus.ALIVE))));
        journals.onStart(start(first));
        assertNull(journals.knowledge(player).other("poi", "aurea"));

        saved(second, "aurea");
        journals.refreshKnowledge();

        assertEquals(List.of("Mira"), journals.knowledge(player).other("poi", "aurea").livingNames());
        assertEquals("Aldric", journals.knowledge(player).characterName());
    }

    private CharacterInfo info(UUID character, String name, CharacterStatus status) {
        return new CharacterInfo(new CharacterRef(owner, character), name, status, null, Set.of(), 0, null);
    }

    private CharacterSessionStartEvent start(UUID character) {
        return new CharacterSessionStartEvent(player, new CharacterSession(new CharacterRef(owner, character), 1));
    }

    private CharacterSessionEndEvent end(UUID character) {
        return new CharacterSessionEndEvent(player, new CharacterSession(new CharacterRef(owner, character), 1),
                CharacterSessionEndEvent.Reason.SWITCH);
    }

    private void saved(UUID character, String discovery) throws IOException {
        PlayerData data = new PlayerData(owner, character, "Steve");
        data.addDiscovery("poi", discovery, "01/10/2026");
        store.save(data);
    }

    private void legacy(String discovery) throws IOException {
        Files.createDirectories(folder.resolve("players"));
        Files.writeString(folder.resolve("players").resolve(owner + ".yml"), """
                name: Steve
                categories:
                  poi:
                    completed: false
                    discoveries:
                    - %s;24/09/2026;0
                """.formatted(discovery));
    }

    private void assertLogged(Level level, String text) {
        assertTrue(logs.stream().anyMatch(record -> record.getLevel().equals(level) && record.getMessage().contains(text)),
                () -> "missing " + level + " log containing: " + text);
    }
}
