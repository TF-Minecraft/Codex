package net.tfminecraft.codex.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyRecipientTest {
    @TempDir
    Path plugins;

    private final UUID owner = UUID.randomUUID();
    private final UUID aldric = UUID.randomUUID();
    private final UUID mira = UUID.randomUUID();

    @Test
    void theFirstChoiceIsPinnedForGood() throws IOException {
        LegacyRecipient recipients = new LegacyRecipient(plugins);

        assertEquals(Optional.of(aldric), recipients.of(owner, () -> Optional.of(aldric)));
        assertEquals(Optional.of(aldric), recipients.of(owner, () -> Optional.of(mira)));
        assertEquals(aldric.toString(), Files.readString(pin()));
        assertFalse(Files.exists(pin().resolveSibling(owner + ".txt.tmp")));
    }

    @Test
    void nobodyToChooseLeavesNoPin() throws IOException {
        assertEquals(Optional.empty(), new LegacyRecipient(plugins).of(owner, Optional::empty));
        assertFalse(Files.exists(pin()));
    }

    @Test
    void anUnreadablePinIsReplaced() throws IOException {
        Files.createDirectories(pin().getParent());
        Files.writeString(pin(), "garbled");

        assertEquals(Optional.of(mira), new LegacyRecipient(plugins).of(owner, () -> Optional.of(mira)));
        assertEquals(mira.toString(), Files.readString(pin()));
    }

    @Test
    void aPinWrittenMeanwhileWins() throws IOException {
        LegacyRecipient recipients = new LegacyRecipient(plugins);

        Optional<UUID> result = recipients.of(owner, () -> {
            try {
                Files.createDirectories(pin().getParent());
                Files.writeString(pin(), aldric.toString());
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
            return Optional.of(mira);
        });

        assertEquals(Optional.of(aldric), result);
        assertTrue(Files.exists(pin()));
        assertFalse(Files.exists(pin().resolveSibling(owner + ".txt.tmp")));
    }

    private Path pin() {
        return plugins.resolve("TLibs").resolve("legacy-recipients").resolve(owner + ".txt");
    }
}
