package net.tfminecraft.codex.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.tfminecraft.tlibs.character.CharacterInfo;
import net.tfminecraft.tlibs.character.CharacterRef;
import net.tfminecraft.tlibs.character.CharacterStatus;
import net.tfminecraft.tlibs.character.Characters;

class HeirsTest {
    private final UUID owner = UUID.randomUUID();

    @Test
    void reviewedHeirWins() {
        CharacterInfo played = info(CharacterStatus.ALIVE, 9000, null);
        CharacterInfo reviewed = info(CharacterStatus.ALIVE, 10, null);

        assertEquals(Optional.of(id(reviewed)),
                Heirs.choose(Optional.of(reviewed.ref()), Optional.of(List.of(played, reviewed))));
    }

    @Test
    void aReviewedHeirDeletedSinceFallsThroughToTheRoster() {
        CharacterInfo left = info(CharacterStatus.ALIVE, 10, null);
        UUID deleted = UUID.randomUUID();

        assertEquals(Optional.of(id(left)),
                Heirs.choose(Optional.of(new CharacterRef(owner, deleted)), Optional.of(List.of(left))));
        assertEquals(Optional.of(deleted), Heirs.choose(Optional.of(new CharacterRef(owner, deleted)), Optional.empty()));
    }

    @Test
    void otherwiseTheMostPlayedLivingCharacter() {
        CharacterInfo dead = info(CharacterStatus.DEAD, 9000, null);
        CharacterInfo short1 = info(CharacterStatus.ALIVE, 100, Instant.parse("2026-10-01T00:00:00Z"));
        CharacterInfo long1 = info(CharacterStatus.ALIVE, 500, null);
        CharacterInfo long2 = info(CharacterStatus.ALIVE, 500, Instant.parse("2026-10-02T00:00:00Z"));

        assertEquals(Optional.of(id(long2)),
                Heirs.choose(Optional.empty(), Optional.of(List.of(dead, short1, long1, long2))));
    }

    @Test
    void anAllDeadAccountFallsBackToItsMostPlayed() {
        CharacterInfo less = info(CharacterStatus.DEAD, 10, null);
        CharacterInfo more = info(CharacterStatus.MISSING, 20, null);

        assertEquals(Optional.of(id(more)), Heirs.choose(Optional.empty(), Optional.of(List.of(less, more))));
    }

    @Test
    void noCharacterMeansNoHeirYet() {
        assertEquals(Optional.empty(), Heirs.choose(Optional.empty(), Optional.of(List.of())));
        assertEquals(Optional.empty(), Heirs.choose(Optional.empty(), Optional.empty()));
    }

    @Test
    void waitsUntilRpCharactersKnowsItsHeirs() {
        UUID reviewed = UUID.randomUUID();
        try (MockedStatic<Characters> characters = mockStatic(Characters.class)) {
            characters.when(Characters::legacyHeirsReady).thenReturn(false);
            assertEquals(Optional.empty(), Heirs.of(owner));

            characters.when(Characters::legacyHeirsReady).thenReturn(true);
            characters.when(() -> Characters.legacyHeir(owner)).thenReturn(Optional.of(new CharacterRef(owner, reviewed)));
            characters.when(() -> Characters.characters(owner)).thenReturn(Optional.empty());
            assertEquals(Optional.of(reviewed), Heirs.of(owner));
        }
    }

    private CharacterInfo info(CharacterStatus status, long playtime, Instant lastPlayed) {
        return new CharacterInfo(new CharacterRef(owner, UUID.randomUUID()), "Name", status, null, Set.of(),
                playtime, lastPlayed);
    }

    private static UUID id(CharacterInfo info) {
        return info.ref().characterId();
    }
}
