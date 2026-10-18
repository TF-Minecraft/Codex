package net.tfminecraft.codex.character;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.tfminecraft.codex.model.data.PlayerData;
import net.tfminecraft.codex.model.data.PlayerDataCategory;
import net.tfminecraft.codex.model.data.PlayerDataDiscovery;

/**
 * Exploration belongs to the account: a character gets the discoveries its account's other characters
 * and its journal from before characters had their own hold, with the reward lines that carry over
 * (Player EXP), once. Entries kept per character, such as research, stay with whoever found them; the
 * others keep seeing them out of character and earn them, rewards included, by finding them themselves.
 */
public final class SharedExploration {
    /** A reward line starting with {@code from} runs for a copied discovery as {@code to} plus the rest of the line. */
    public record Rewrite(String from, String to) {
    }

    /** A discovery copied into a journal. */
    public record Copy(String category, String discovery) {
    }

    /**
     * {@code keptPerCharacter} lists categories and {@code category:discovery} entries that are not shared.
     * Reward lines without a matching rewrite are skipped when a discovery is copied.
     */
    public record Settings(boolean enabled, Set<String> keptPerCharacter, List<Rewrite> rewards) {
        public static final Settings OFF = new Settings(false, Set.of(), List.of());

        public Settings {
            keptPerCharacter = Set.copyOf(keptPerCharacter);
            rewards = List.copyOf(rewards);
        }

        public boolean shared(String category, String discovery) {
            return enabled && !keptPerCharacter.contains(category)
                    && !keptPerCharacter.contains(AccountKnowledge.key(category, discovery));
        }

        /** The line to run for a copied discovery's reward line, or null when it does not carry over. */
        public String reward(String line) {
            String trimmed = line.trim();
            for (Rewrite rewrite : rewards) {
                if (trimmed.startsWith(rewrite.from())) {
                    return rewrite.to() + trimmed.substring(rewrite.from().length());
                }
            }
            return null;
        }
    }

    private SharedExploration() {
    }

    /**
     * Adds to the journal the shared discoveries it lacks from the other journals, keeping the date
     * each was first found. Marks the journal modified when anything was added.
     * @return the copies, in the order they were added
     */
    static List<Copy> copy(PlayerData journal, List<List<PlayerDataCategory>> others, Settings settings) {
        List<Copy> copies = new ArrayList<>();
        for (List<PlayerDataCategory> other : others) {
            for (PlayerDataCategory category : other) {
                for (PlayerDataDiscovery discovery : category.getDiscoveries()) {
                    String id = discovery.getDiscoveryName();
                    if (settings.shared(category.getName(), id) && !journal.hasDiscovery(category.getName(), id)) {
                        journal.addDiscovery(category.getName(), id, discovery.getDiscoverDate());
                        copies.add(new Copy(category.getName(), id));
                    }
                }
            }
        }
        if (!copies.isEmpty()) {
            journal.setModified(true);
        }
        return copies;
    }
}
