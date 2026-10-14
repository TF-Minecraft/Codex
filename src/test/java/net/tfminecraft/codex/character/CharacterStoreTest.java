package net.tfminecraft.codex.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.tfminecraft.codex.model.data.PlayerData;
import net.tfminecraft.codex.model.data.PlayerDataCategory;
import net.tfminecraft.codex.model.data.PlayerDataDiscovery;

class CharacterStoreTest {
    @TempDir
    Path folder;

    private CharacterStore store;
    private final UUID owner = UUID.randomUUID();
    private final UUID character = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        store = new CharacterStore(folder);
    }

    @Test
    void newCharacterStartsEmptyAndIsSavedLater() throws IOException {
        PlayerData data = store.load(owner, character, "Steve");

        assertEquals(owner, data.getUuid());
        assertEquals(character, data.getCharacterId());
        assertEquals("Steve", data.getName());
        assertTrue(data.getCategories().isEmpty());
        assertTrue(data.isModified());
    }

    @Test
    void savedJournalLoadsBackWithEveryField() throws IOException {
        PlayerData data = store.load(owner, character, "Steve");
        data.addDiscovery("poi", "syrindell", "24/09/2026");
        data.getCategory("poi").getDiscovery("syrindell").setMillisActionsExecuted(42);
        data.completeCategory("poi");

        store.save(data);

        assertFalse(data.isModified());
        String text = Files.readString(store.characterFile(character));
        assertTrue(text.contains("owner: " + owner));
        assertTrue(text.contains("name: Steve"));
        assertFalse(Files.exists(folder.resolve("characters").resolve(character + ".yml.tmp")));

        PlayerData loaded = store.load(owner, character, "Alex");
        assertFalse(loaded.isModified());
        assertEquals("Alex", loaded.getName());
        PlayerDataCategory poi = loaded.getCategory("poi");
        assertTrue(poi.isCompleted());
        PlayerDataDiscovery found = poi.getDiscovery("syrindell");
        assertEquals("24/09/2026", found.getDiscoverDate());
        assertEquals(42, found.getMillisActionsExecuted());
    }

    @Test
    void oldEntriesWithoutMillisOrDateStillLoad() throws IOException {
        write("characters", character, """
                categories:
                  poi:
                    discoveries:
                    - aurea;01/10/2026
                    - vard
                    - thalendor;02/10/2026;soon
                    - ;03/10/2026;0
                """);

        PlayerDataCategory poi = store.load(owner, character, "Steve").getCategory("poi");

        assertFalse(poi.isCompleted());
        assertEquals(3, poi.getDiscoveries().size());
        assertEquals(0, poi.getDiscovery("aurea").getMillisActionsExecuted());
        assertEquals("", poi.getDiscovery("vard").getDiscoverDate());
        assertEquals(0, poi.getDiscovery("thalendor").getMillisActionsExecuted());
    }

    @Test
    void unreadableFilesAreErrorsNotEmptyJournals() throws IOException {
        write("characters", character, "categories: [unclosed");
        write("players", owner, "name: [unclosed");

        assertThrows(IOException.class, () -> store.load(owner, character, "Steve"));
        assertThrows(IOException.class, () -> store.readLegacy(owner));
    }

    @Test
    void failedSaveKeepsTheJournalModified() throws IOException {
        Files.writeString(folder.resolve("characters"), "not a folder");
        PlayerData data = new PlayerData(owner, character, "Steve");
        data.setModified(true);

        assertThrows(IOException.class, () -> store.save(data));
        assertTrue(data.isModified());
    }

    @Test
    void legacyOwnersAreTheAccountFiles() throws IOException {
        assertTrue(store.legacyOwners().isEmpty());

        write("players", owner, "name: Steve\n");
        Files.writeString(folder.resolve("players").resolve("notes.yml"), "x: 1\n");
        Files.writeString(folder.resolve("players").resolve(UUID.randomUUID() + ".txt"), "x: 1\n");

        assertEquals(List.of(owner), store.legacyOwners());
    }

    @Test
    void legacyJournalIsReadThenRetired() throws IOException {
        assertFalse(store.hasLegacy(owner));
        write("players", owner, """
                name: Steve
                categories:
                  achievements:
                    completed: true
                    discoveries:
                    - die;20/09/2026;0
                """);
        write("players-migrated", owner, "name: Older\n");

        CharacterStore.LegacyJournal legacy = store.readLegacy(owner);

        assertTrue(store.hasLegacy(owner));
        assertEquals("Steve", legacy.name());
        assertTrue(legacy.categories().get(0).isCompleted());
        assertTrue(legacy.categories().get(0).hasDiscovery("die"));

        store.retireLegacy(owner);

        assertFalse(store.hasLegacy(owner));
        assertTrue(Files.readString(folder.resolve("players-migrated").resolve(owner + ".yml")).contains("Steve"));
    }

    @Test
    void legacyJournalWithoutCategoriesIsEmpty() throws IOException {
        write("players", owner, "x: 1\n");

        CharacterStore.LegacyJournal legacy = store.readLegacy(owner);

        assertNull(legacy.name());
        assertEquals(new ArrayList<>(), legacy.categories());
    }

    private void write(String directory, UUID id, String text) throws IOException {
        Path dir = folder.resolve(directory);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(id + ".yml"), text);
    }
}
