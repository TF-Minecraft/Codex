package net.tfminecraft.codex.config;

import net.tfminecraft.codex.Codex;
import net.tfminecraft.codex.character.SharedExploration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainConfigManager {
    /** Entries that stay with the character that found them: research, and achievements counting research. */
    public static final List<String> KEPT_PER_CHARACTER = List.of("research", "achievements:5_research",
            "achievements:10_research", "achievements:25_research", "achievements:all_research",
            "achievements:full_codex");
    /** Player EXP of a shared discovery goes through RPCharacters, which keeps what a capped class cannot take yet. */
    public static final String EXP_FROM = "console_command: mmocore admin exp give %player% main ";
    public static final String EXP_TO = "console_command: rpcharacter admin carryexp %player% ";

    private Codex plugin;
    private CommonConfig configFile;

    private String discoveriesDateFormat;
    private int playerDataSave;
    private String progressBarPlaceholderFillSymbol;
    private String progressBarPlaceholderEmptySymbol;
    private int progressBarPlaceholderAmount;
    private boolean useMiniMessage;
    private int configVersion;
    private SharedExploration.Settings sharedExploration = SharedExploration.Settings.OFF;

    public MainConfigManager(Codex plugin){
        this.plugin = plugin;
        this.configFile = new CommonConfig("config.yml",plugin,null,false);
        configFile.registerConfig();
        checkUpdate();
    }

    public void configure() {
        FileConfiguration config = configFile.getConfig();
        discoveriesDateFormat = config.getString("discoveries_date_format");
        playerDataSave = config.getInt("player_data_save");
        progressBarPlaceholderFillSymbol = config.getString("progress_bar_placeholder.filled_symbol");
        progressBarPlaceholderEmptySymbol = config.getString("progress_bar_placeholder.empty_symbol");
        progressBarPlaceholderAmount = config.getInt("progress_bar_placeholder.amount");
        useMiniMessage = config.getBoolean("use_minimessage");
        configVersion = config.getInt("config_version");
        sharedExploration = sharedExploration(config.getConfigurationSection("shared_discoveries"));
    }

    private static SharedExploration.Settings sharedExploration(ConfigurationSection section) {
        if (section == null) {
            return SharedExploration.Settings.OFF;
        }
        List<SharedExploration.Rewrite> rewards = new ArrayList<>();
        for (Map<?, ?> reward : section.getMapList("rewards")) {
            if (reward.get("from") instanceof String from && !from.isBlank() && reward.get("to") instanceof String to) {
                rewards.add(new SharedExploration.Rewrite(from, to));
            }
        }
        return new SharedExploration.Settings(section.getBoolean("enabled"),
                new HashSet<>(section.getStringList("keep_per_character")), rewards);
    }

    public boolean reloadConfig(){
        if(!configFile.reloadConfig()){
            return false;
        }
        configure();
        return true;
    }

    public void checkUpdate(){
        Path pathConfig = Paths.get(configFile.getRoute());
        try{
            String text = new String(Files.readAllBytes(pathConfig));
            if(!text.contains("use_minimessage:")){
                getConfig().set("use_minimessage",false);
                configFile.saveConfig();
            }
            if(!text.contains("shared_discoveries:")){
                FileConfiguration config = getConfig();
                config.set("shared_discoveries.enabled",true);
                config.set("shared_discoveries.keep_per_character",KEPT_PER_CHARACTER);
                Map<String,Object> exp = new LinkedHashMap<>();
                exp.put("from",EXP_FROM);
                exp.put("to",EXP_TO);
                config.set("shared_discoveries.rewards",List.of(exp));
                config.setComments("shared_discoveries",List.of(
                        "Exploration belongs to the account. When a character starts playing, it gets the",
                        "discoveries its account's other characters (alive or dead) and its journal from before",
                        "characters made, except the categories and category:discovery entries kept per character.",
                        "Those stay with whoever found them; the others see them out of character.",
                        "A copied discovery runs only its reward lines starting with a rewards 'from', as 'to'",
                        "plus the rest of the line (Player EXP goes through RPCharacters, which keeps what a",
                        "capped class can't take yet). Its other reward lines are skipped."));
                configFile.saveConfig();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public CommonConfig getConfigFile() {
        return configFile;
    }

    public FileConfiguration getConfig(){
        return configFile.getConfig();
    }

    public String getDiscoveriesDateFormat() {
        return discoveriesDateFormat;
    }

    public int getPlayerDataSave() {
        return playerDataSave;
    }

    public int getConfigVersion() {
        return configVersion;
    }

    public String getProgressBarPlaceholderFillSymbol() {
        return progressBarPlaceholderFillSymbol;
    }

    public String getProgressBarPlaceholderEmptySymbol() {
        return progressBarPlaceholderEmptySymbol;
    }

    public int getProgressBarPlaceholderAmount() {
        return progressBarPlaceholderAmount;
    }

    public SharedExploration.Settings getSharedExploration() {
        return sharedExploration;
    }

    public boolean isUseMiniMessage() {
        return useMiniMessage;
    }
}
