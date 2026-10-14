package net.tfminecraft.codex.character;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.tfminecraft.codex.model.data.PlayerData;
import net.tfminecraft.codex.model.data.PlayerDataCategory;
import net.tfminecraft.codex.model.data.PlayerDataDiscovery;

/**
 * What the account knows beyond the played character, read when a character's session starts: its
 * other characters' journals and its journal from before characters had their own. Every such entry
 * stays readable for out-of-character reference; only living characters who found it themselves can
 * use it in character. The played character's own journal is live, so it is checked separately.
 */
public final class AccountKnowledge {
    /** A discovery known elsewhere on the account: a date it was found, and which living characters know it. */
    public record Known(String date, List<String> livingNames) {
    }

    private final String characterName;
    private final Map<String, Known> others;

    AccountKnowledge(String characterName, Map<String, Known> others) {
        this.characterName = characterName;
        this.others = others;
    }

    public static AccountKnowledge empty(String characterName) {
        return new AccountKnowledge(characterName, Map.of());
    }

    public String characterName() {
        return characterName;
    }

    /** The other characters' discoveries, keyed {@code category:discovery}. */
    public Known other(String category, String discovery) {
        return others.get(key(category, discovery));
    }

    /** Living characters who know the entry in character, the played character first when it does. */
    public List<String> knownInCharacterBy(PlayerData current, String category, String discovery) {
        List<String> names = new ArrayList<>();
        if (current != null && current.hasDiscovery(category, discovery)) {
            names.add(characterName);
        }
        Known known = other(category, discovery);
        if (known != null) {
            names.addAll(known.livingNames());
        }
        return names;
    }

    static String key(String category, String discovery) {
        return category + ":" + discovery;
    }

    /** Collects journals. Each is added once; dead characters and the old account journal count but are not named. */
    static final class Builder {
        private final Map<String, String> dates = new HashMap<>();
        private final Map<String, List<String>> names = new HashMap<>();

        void add(List<PlayerDataCategory> categories, String name, boolean alive) {
            for (PlayerDataCategory category : categories) {
                for (PlayerDataDiscovery discovery : category.getDiscoveries()) {
                    String key = key(category.getName(), discovery.getDiscoveryName());
                    dates.putIfAbsent(key, discovery.getDiscoverDate());
                    List<String> known = names.computeIfAbsent(key, ignored -> new ArrayList<>());
                    if (alive) {
                        known.add(name);
                    }
                }
            }
        }

        AccountKnowledge build(String characterName) {
            Map<String, Known> others = new HashMap<>();
            names.forEach((key, known) -> others.put(key, new Known(dates.get(key), List.copyOf(known))));
            return new AccountKnowledge(characterName, Map.copyOf(others));
        }
    }
}
