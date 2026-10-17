package net.tfminecraft.codex.config;

import net.tfminecraft.codex.Codex;
import net.tfminecraft.codex.managers.MessagesManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MessagesConfigManager {
    public static final String KNOWN_BY = "&7%characters% can use this in character.";
    public static final String KNOWN_BY_NOBODY = "&7No character can use this in character.";
    public static final String OUT_OF_CHARACTER_ONLY = "&8&o%character% knows this only out of character.";
    public static final String NO_CHARACTER_IN_PLAY = "&cChoose a character to open the Codex.";
    public static final String OUT_OF_CHARACTER_ADVICE =
            "&8&oUse common sense: play %character% as not knowing this until they learn it in character.";
    public static final String SHARED_DISCOVERIES =
            "&7%amount% Codex entries carried over from your other characters, with their Player EXP.";


    private Codex plugin;
    private CommonConfig configFile;

    public MessagesConfigManager(Codex plugin){
        this.plugin = plugin;
        this.configFile = new CommonConfig("messages.yml",plugin,null,false);
        configFile.registerConfig();
        checkUpdates();
    }

    public void configure() {
        FileConfiguration config = configFile.getConfig();

        MessagesManager messagesManager = new MessagesManager();
        messagesManager.setPrefix(config.getString("prefix"));
        messagesManager.setTimeSeconds(config.getString("seconds"));
        messagesManager.setTimeMinutes(config.getString("minutes"));
        messagesManager.setTimeHours(config.getString("hours"));
        messagesManager.setTimeDays(config.getString("days"));

        plugin.setMessagesManager(messagesManager);
    }

    public CommonConfig getConfigFile() {
        return configFile;
    }

    public boolean reloadConfig(){
        if(!configFile.reloadConfig()){
            return false;
        }
        configure();
        return true;
    }

    public void checkUpdates(){
        Path pathConfig = Paths.get(configFile.getRoute());
        try{
            String text = new String(Files.readAllBytes(pathConfig));
            FileConfiguration config = configFile.getConfig();

            if(!text.contains("knownInCharacterBy:")){
                config.set("knownInCharacterBy",KNOWN_BY);
                config.set("knownInCharacterByNobody",KNOWN_BY_NOBODY);
                config.set("knownOutOfCharacterOnly",OUT_OF_CHARACTER_ONLY);
                configFile.saveConfig();
            }
            if(!text.contains("knownOutOfCharacterOnlyAdvice:")){
                config.set("knownOutOfCharacterOnlyAdvice",OUT_OF_CHARACTER_ADVICE);
                configFile.saveConfig();
            }
            if(!text.contains("sharedDiscoveries:")){
                config.set("sharedDiscoveries",SHARED_DISCOVERIES);
                configFile.saveConfig();
            }
            if(!text.contains("noCharacterInPlay:")){
                config.set("noCharacterInPlay",NO_CHARACTER_IN_PLAY);
                configFile.saveConfig();
            }
            if(!text.contains("clickActionsCooldown:")){
                config.set("clickActionsCooldown","&cYou must wait &7%time% &cbefore using this item again.");
                config.set("seconds","s");
                config.set("minutes","m");
                config.set("hours","h");
                config.set("days","d");
                configFile.saveConfig();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
