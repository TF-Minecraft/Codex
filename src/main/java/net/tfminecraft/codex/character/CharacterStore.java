package net.tfminecraft.codex.character;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import net.tfminecraft.codex.model.data.PlayerData;
import net.tfminecraft.codex.model.data.PlayerDataCategory;
import net.tfminecraft.codex.model.data.PlayerDataDiscovery;

/**
 * Discovery files. Each character has {@code characters/<character>.yml}. {@code players/<account>.yml}
 * holds what an account found before characters had their own; it stays as the account's
 * out-of-character reference and no character inherits it. Main thread only.
 */
public final class CharacterStore {
    public static final String CHARACTERS = "characters";
    public static final String LEGACY = "players";

    private final Path characters;
    private final Path legacy;

    public CharacterStore(Path dataFolder) {
        this.characters = dataFolder.resolve(CHARACTERS);
        this.legacy = dataFolder.resolve(LEGACY);
    }

    /** What an account found before characters had their own journals, with the account name it was saved under. */
    public record LegacyJournal(String name, List<PlayerDataCategory> categories) {
    }

    public Path characterFile(UUID characterId) {
        return characters.resolve(characterId + ".yml");
    }

    /** The character's journal. A new journal is marked modified, so every played character gets a file. */
    public PlayerData load(UUID owner, UUID characterId, String name) throws IOException {
        PlayerData data = new PlayerData(owner, characterId, name);
        Path file = characterFile(characterId);
        if (Files.exists(file)) {
            data.setCategories(categories(read(file)));
        } else {
            data.setModified(true);
        }
        return data;
    }

    /** Writes the journal through a temporary file, so a failed write leaves the old file whole. */
    public void save(PlayerData data) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("owner", data.getUuid().toString());
        yaml.set("name", data.getName());
        for (PlayerDataCategory category : data.getCategories()) {
            String path = "categories." + category.getName();
            yaml.set(path + ".completed", category.isCompleted());
            yaml.set(path + ".discoveries", category.getDiscoveriesToStringList());
        }
        Files.createDirectories(characters);
        Path target = characterFile(data.getCharacterId());
        Path temporary = characters.resolve(data.getCharacterId() + ".yml.tmp");
        Files.writeString(temporary, yaml.saveToString(), StandardCharsets.UTF_8);
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        data.setModified(false);
    }

    public boolean hasLegacy(UUID owner) {
        return Files.isRegularFile(legacyFile(owner));
    }

    public LegacyJournal readLegacy(UUID owner) throws IOException {
        YamlConfiguration yaml = read(legacyFile(owner));
        return new LegacyJournal(yaml.getString("name"), categories(yaml));
    }

    private Path legacyFile(UUID owner) {
        return legacy.resolve(owner + ".yml");
    }

    private static YamlConfiguration read(Path file) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
        } catch (InvalidConfigurationException ex) {
            throw new IOException("Unreadable discovery file " + file, ex);
        }
        return yaml;
    }

    /** Discoveries are saved as {@code id;date;millis}; older files may lack the millis. */
    private static List<PlayerDataCategory> categories(YamlConfiguration yaml) {
        List<PlayerDataCategory> categories = new ArrayList<>();
        ConfigurationSection section = yaml.getConfigurationSection("categories");
        if (section == null) {
            return categories;
        }
        for (String name : section.getKeys(false)) {
            ArrayList<PlayerDataDiscovery> discoveries = new ArrayList<>();
            for (String entry : section.getStringList(name + ".discoveries")) {
                String[] parts = entry.split(";", -1);
                if (parts[0].isBlank()) {
                    continue;
                }
                String date = parts.length > 1 ? parts[1] : "";
                discoveries.add(new PlayerDataDiscovery(parts[0], date, parts.length > 2 ? millis(parts[2]) : 0));
            }
            categories.add(new PlayerDataCategory(name, section.getBoolean(name + ".completed"), discoveries));
        }
        return categories;
    }

    private static long millis(String text) {
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
