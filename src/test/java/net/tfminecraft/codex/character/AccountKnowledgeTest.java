package net.tfminecraft.codex.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.tfminecraft.codex.model.data.PlayerData;
import net.tfminecraft.codex.model.data.PlayerDataCategory;
import net.tfminecraft.codex.model.data.PlayerDataDiscovery;

class AccountKnowledgeTest {
    @Test
    void namesLivingCharactersWithThePlayedOneFirst() {
        AccountKnowledge.Builder builder = new AccountKnowledge.Builder();
        builder.add(journal("aurea", "01/10/2026"), "Mira", true);
        builder.add(journal("aurea", "05/10/2026"), "Old Tom", false);
        builder.add(journal("aurea", "06/10/2026"), "Hazel", true);
        AccountKnowledge knowledge = builder.build("Aldric");
        PlayerData current = new PlayerData(UUID.randomUUID(), UUID.randomUUID(), "Steve");
        current.addDiscovery("poi", "aurea", "14/10/2026");

        assertEquals("Aldric", knowledge.characterName());
        assertEquals("01/10/2026", knowledge.other("poi", "aurea").date());
        assertEquals(List.of("Aldric", "Mira", "Hazel"), knowledge.knownInCharacterBy(current, "poi", "aurea"));
        assertEquals(List.of("Mira", "Hazel"), knowledge.knownInCharacterBy(null, "poi", "aurea"));
    }

    @Test
    void entriesOnlyDeadCharactersFoundAreSeenButNamedByNobody() {
        AccountKnowledge.Builder builder = new AccountKnowledge.Builder();
        builder.add(journal("vard", "01/10/2026"), "Old Tom", false);
        AccountKnowledge knowledge = builder.build("Aldric");

        assertEquals(List.of(), knowledge.other("poi", "vard").livingNames());
        assertEquals(List.of(), knowledge.knownInCharacterBy(null, "poi", "vard"));
    }

    @Test
    void anEmptyAccountKnowsNothingElse() {
        AccountKnowledge knowledge = AccountKnowledge.empty("Aldric");

        assertNull(knowledge.other("poi", "aurea"));
        assertEquals(List.of(), knowledge.knownInCharacterBy(null, "poi", "aurea"));
    }

    private static List<PlayerDataCategory> journal(String discovery, String date) {
        ArrayList<PlayerDataDiscovery> found = new ArrayList<>(List.of(new PlayerDataDiscovery(discovery, date, 0)));
        return List.of(new PlayerDataCategory("poi", false, found));
    }
}
