package net.tfminecraft.codex.character;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The one character that receives an account's data from before characters had their own, pinned
 * in {@code plugins/TLibs/legacy-recipients/<account>.txt}. Codex and ConditionalEvents both read
 * this file, so they pick the same character, and retries never pick another. Main thread only.
 */
public final class LegacyRecipient {
    static final String FOLDER = "TLibs/legacy-recipients";

    private final Path folder;

    public LegacyRecipient(Path pluginsFolder) {
        this.folder = pluginsFolder.resolve(FOLDER);
    }

    /**
     * The pinned character; if none is pinned yet, pins and returns the chosen one. Empty when none
     * is pinned and nobody can be chosen yet.
     */
    public Optional<UUID> of(UUID owner, Supplier<Optional<UUID>> choose) throws IOException {
        Path file = folder.resolve(owner + ".txt");
        Optional<UUID> pinned = read(file);
        if (pinned.isPresent()) {
            return pinned;
        }
        boolean unreadable = Files.exists(file);
        Optional<UUID> chosen = choose.get();
        if (chosen.isEmpty()) {
            return chosen;
        }
        Files.createDirectories(folder);
        Path temporary = folder.resolve(owner + ".txt.tmp");
        Files.writeString(temporary, chosen.get().toString(), StandardCharsets.UTF_8);
        try {
            if (unreadable) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } else {
                // Without REPLACE_EXISTING the move refuses a pin written meanwhile.
                Files.move(temporary, file);
            }
        } catch (FileAlreadyExistsException pinnedMeanwhile) {
            Files.deleteIfExists(temporary);
            return read(file).or(() -> chosen);
        }
        return chosen;
    }

    private static Optional<UUID> read(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(Files.readString(file, StandardCharsets.UTF_8).trim()));
        } catch (IllegalArgumentException unreadable) {
            return Optional.empty();
        }
    }
}
