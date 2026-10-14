package net.tfminecraft.codex.character;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.tfminecraft.codex.model.data.PlayerData;
import net.tfminecraft.codex.model.data.PlayerDataCategory;
import net.tfminecraft.codex.model.data.PlayerDataDiscovery;

/**
 * What the account's other characters have discovered, read when a character's session starts.
 * An entry any character found, alive or dead, stays readable for out-of-character reference;
 * only living characters can use it in character. The played character's own journal is live,
 * so it is checked separately.
 */
public final class AccountKnowledge {
    /** A discovery made by other characters: a date one of them found it, and which living ones know it. */
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

    /** Collects other characters' journals. Each is added once; dead characters count but are not named. */
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
