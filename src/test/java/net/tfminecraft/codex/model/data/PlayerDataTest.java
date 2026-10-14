package net.tfminecraft.codex.model.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlayerDataTest {
    @Test
    void absorbAddsOnlyMissingDiscoveriesAndKeepsCompletion() {
        PlayerData own = new PlayerData(UUID.randomUUID(), UUID.randomUUID(), "Steve");
        own.addDiscovery("poi", "vard", "14/10/2026");
        ArrayList<PlayerDataDiscovery> poi = new ArrayList<>(List.of(
                new PlayerDataDiscovery("vard", "01/09/2026", 5),
                new PlayerDataDiscovery("aurea", "02/09/2026", 7)));
        ArrayList<PlayerDataDiscovery> achievements = new ArrayList<>(List.of(new PlayerDataDiscovery("die", "03/09/2026", 0)));

        int added = own.absorb(List.of(new PlayerDataCategory("poi", false, poi),
                new PlayerDataCategory("achievements", true, achievements)));

        assertEquals(2, added);
        assertTrue(own.isModified());
        assertEquals("14/10/2026", own.getDiscovery("poi", "vard").getDiscoverDate());
        assertEquals(7, own.getDiscovery("poi", "aurea").getMillisActionsExecuted());
        assertTrue(own.isCategoryCompleted("achievements"));
        assertFalse(own.isCategoryCompleted("poi"));
    }
}
