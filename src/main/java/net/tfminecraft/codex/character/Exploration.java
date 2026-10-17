package net.tfminecraft.codex.character;

import java.util.List;

import org.bukkit.entity.Player;

/** What sharing discoveries needs from the rest of Codex: its settings, the rewards and the scheduler. */
public interface Exploration {
    SharedExploration.Settings settings();

    /**
     * Runs the copies' reward lines that carry over and those of any category they complete, then
     * tells the player what arrived. The copies are already in the player's journal.
     */
    void reward(Player player, List<SharedExploration.Copy> copies);

    /** Runs the task on the main thread after the given number of ticks. */
    void later(long ticks, Runnable task);
}
