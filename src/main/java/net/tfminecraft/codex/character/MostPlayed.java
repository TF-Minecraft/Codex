package net.tfminecraft.codex.character;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.tfminecraft.tlibs.character.CharacterInfo;

/** The account's most-played living character, else its most-played character. */
final class MostPlayed {
    private static final Comparator<CharacterInfo> PLAYTIME = Comparator
            .comparingLong(CharacterInfo::playtimeSeconds)
            .thenComparing(info -> info.lastPlayedAt() == null ? Instant.MIN : info.lastPlayedAt());

    private MostPlayed() {
    }

    /** Empty when the roster is unknown or the account has no character. */
    static Optional<UUID> of(Optional<List<CharacterInfo>> roster) {
        List<CharacterInfo> all = roster.orElse(List.of());
        Optional<CharacterInfo> best = all.stream().filter(CharacterInfo::alive).max(PLAYTIME);
        if (best.isEmpty()) {
            best = all.stream().max(PLAYTIME);
        }
        return best.map(info -> info.ref().characterId());
    }
}
