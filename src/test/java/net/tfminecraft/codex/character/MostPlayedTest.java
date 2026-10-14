package net.tfminecraft.codex.character;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.tfminecraft.tlibs.character.CharacterInfo;
import net.tfminecraft.tlibs.character.CharacterRef;
import net.tfminecraft.tlibs.character.CharacterStatus;

class MostPlayedTest {
    private final UUID owner = UUID.randomUUID();

    @Test
    void theMostPlayedLivingCharacterWins() {
        CharacterInfo dead = info(CharacterStatus.DEAD, 9000, null);
        CharacterInfo less = info(CharacterStatus.ALIVE, 100, Instant.parse("2026-10-01T00:00:00Z"));
        CharacterInfo tied = info(CharacterStatus.ALIVE, 500, null);
        CharacterInfo later = info(CharacterStatus.ALIVE, 500, Instant.parse("2026-10-02T00:00:00Z"));

        assertEquals(Optional.of(id(later)), MostPlayed.of(Optional.of(List.of(dead, less, tied, later))));
    }

    @Test
    void anAllDeadAccountFallsBackToItsMostPlayed() {
        CharacterInfo less = info(CharacterStatus.DEAD, 10, null);
        CharacterInfo more = info(CharacterStatus.MISSING, 20, null);

        assertEquals(Optional.of(id(more)), MostPlayed.of(Optional.of(List.of(less, more))));
    }

    @Test
    void noRosterOrNoCharacterMeansNobody() {
        assertEquals(Optional.empty(), MostPlayed.of(Optional.empty()));
        assertEquals(Optional.empty(), MostPlayed.of(Optional.of(List.of())));
    }

    private CharacterInfo info(CharacterStatus status, long playtime, Instant lastPlayed) {
        return new CharacterInfo(new CharacterRef(owner, UUID.randomUUID()), "Name", status, null, Set.of(), playtime, lastPlayed);
    }

    private static UUID id(CharacterInfo info) {
        return info.ref().characterId();
    }
}
