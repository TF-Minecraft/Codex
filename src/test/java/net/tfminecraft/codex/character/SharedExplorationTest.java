package net.tfminecraft.codex.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.tfminecraft.codex.model.data.PlayerData;
import net.tfminecraft.codex.model.data.PlayerDataCategory;
import net.tfminecraft.codex.model.data.PlayerDataDiscovery;

class SharedExplorationTest {
    private final SharedExploration.Settings settings = new SharedExploration.Settings(true,
            new HashSet<>(Set.of("research", "achievements:5_research")),
            new ArrayList<>(List.of(new SharedExploration.Rewrite("console_command: mmocore admin exp give %player% main ",
                    "console_command: rpcharacter admin carryexp %player% "),
                    new SharedExploration.Rewrite("message: ", "message: "))));

    @Test
    void entriesKeptPerCharacterAreNeverShared() {
        assertTrue(settings.shared("poi", "aurea"));
        assertTrue(settings.shared("achievements", "die"));
        assertFalse(settings.shared("research", "ignitium"));
        assertFalse(settings.shared("achievements", "5_research"));
        assertFalse(SharedExploration.Settings.OFF.shared("poi", "aurea"));
        assertThrows(UnsupportedOperationException.class, () -> settings.keptPerCharacter().add("poi"));
        assertThrows(UnsupportedOperationException.class, () -> settings.rewards().clear());
    }

    @Test
    void onlyRewardLinesWithARewriteCarryOver() {
        assertEquals("console_command: rpcharacter admin carryexp %player% 50",
                settings.reward("  console_command: mmocore admin exp give %player% main 50"));
        assertEquals("message: hi", settings.reward("message: hi"));
        assertNull(settings.reward("title: 20;60;20;&a&lCODEX UPDATED;&7%name%"));
        assertNull(settings.reward("console_command: mmocore admin exp give %player% profession 50"));
    }

    @Test
    void copiesAddOnlyMissingSharedEntriesWithTheirDates() {
        UUID owner = UUID.randomUUID();
        PlayerData journal = new PlayerData(owner, UUID.randomUUID(), "Steve");
        journal.addDiscovery("poi", "aurea", "05/10/2026");
        List<PlayerDataCategory> mira = List.of(category("poi", "aurea", "vard"), category("research", "ignitium"));
        List<PlayerDataCategory> tom = List.of(category("poi", "vard", "syrindell"), category("achievements", "5_research", "die"));

        List<SharedExploration.Copy> copies = SharedExploration.copy(journal, List.of(mira, tom), settings);

        assertEquals(List.of(new SharedExploration.Copy("poi", "vard"), new SharedExploration.Copy("poi", "syrindell"),
                new SharedExploration.Copy("achievements", "die")), copies);
        assertEquals("05/10/2026", journal.getDiscovery("poi", "aurea").getDiscoverDate(), "Own entries keep their date");
        assertEquals("01/10/2026", journal.getDiscovery("poi", "vard").getDiscoverDate());
        assertFalse(journal.hasDiscovery("research", "ignitium"));
        assertTrue(journal.isModified());

        journal.setModified(false);
        assertEquals(List.of(), SharedExploration.copy(journal, List.of(mira, tom), settings));
        assertFalse(journal.isModified(), "Nothing new leaves the journal unchanged");
    }

    private static PlayerDataCategory category(String name, String... discoveries) {
        ArrayList<PlayerDataDiscovery> found = new ArrayList<>();
        for (String discovery : discoveries) {
            found.add(new PlayerDataDiscovery(discovery, "01/10/2026", 0));
        }
        return new PlayerDataCategory(name, false, found);
    }
}
