package net.tfminecraft.codex.character;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.tfminecraft.tlibs.character.CharacterInfo;
import net.tfminecraft.tlibs.character.CharacterRef;
import net.tfminecraft.tlibs.character.Characters;

/** Which character inherits an account's pre-character discoveries. Main thread only. */
final class Heirs {
    private static final Comparator<CharacterInfo> MOST_PLAYED = Comparator
            .comparingLong(CharacterInfo::playtimeSeconds)
            .thenComparing(info -> info.lastPlayedAt() == null ? Instant.MIN : info.lastPlayedAt());

    private Heirs() {
    }

    /** Empty until RPCharacters knows its heirs, and for accounts with no character yet. */
    static Optional<UUID> of(UUID owner) {
        if (!Characters.legacyHeirsReady()) {
            return Optional.empty();
        }
        return choose(Characters.legacyHeir(owner), Characters.characters(owner));
    }

    /**
     * The reviewed heir RPCharacters gives money and bodies to. Accounts that joined after that review
     * have none, and a reviewed heir can since have been deleted; then the most-played living character
     * inherits, else the most-played character.
     */
    static Optional<UUID> choose(Optional<CharacterRef> reviewed, Optional<List<CharacterInfo>> characters) {
        List<CharacterInfo> all = characters.orElse(List.of());
        // A reviewed heir deleted since the review falls through to the rule below.
        if (reviewed.isPresent() && (characters.isEmpty()
                || all.stream().anyMatch(info -> info.ref().characterId().equals(reviewed.get().characterId())))) {
            return Optional.of(reviewed.get().characterId());
        }
        Optional<CharacterInfo> heir = all.stream().filter(CharacterInfo::alive).max(MOST_PLAYED);
        if (heir.isEmpty()) {
            heir = all.stream().max(MOST_PLAYED);
        }
        return heir.map(info -> info.ref().characterId());
    }
}
