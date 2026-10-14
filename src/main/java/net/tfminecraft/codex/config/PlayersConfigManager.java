package net.tfminecraft.codex.config;

import net.tfminecraft.codex.Codex;
import net.tfminecraft.codex.character.CharacterStore;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.*;

/** The per-character discovery files in {@code characters/}; see {@link net.tfminecraft.codex.character.CharacterStore}. */
public class PlayersConfigManager extends DataFolderConfigManager {

    public PlayersConfigManager(Codex plugin, String folderName) {
        super(plugin, folderName);
    }

    @Override
    public void loadConfigs(){
        // No use for player config
    }

    @Override
    public void saveConfigs(){
        plugin.getCharacterJournals().saveAll();
    }

    /** Resets every character's file and every account file still waiting to move to its heir. */
    public void resetDataForAllPlayers(String categoryName,String discoveryName){
        ArrayList<CommonConfig> configs = getConfigs();
        configs.addAll(getConfigs(CharacterStore.LEGACY));
        for(CommonConfig commonConfig : configs) {
            FileConfiguration config = commonConfig.getConfig();

            if(categoryName == null){
                config.set("categories",null);
                commonConfig.saveConfig();
                continue;
            }

            if(discoveryName == null){
                if(!config.contains("categories."+categoryName)){
                    continue;
                }
                config.set("categories."+categoryName,null);
                commonConfig.saveConfig();
                continue;
            }

            List<String> discoveries = config.getStringList("categories."+categoryName+".discoveries");
            int findIndex = -1;
            for(int i=0;i<discoveries.size();i++){
                if(discoveries.get(i).split(";")[0].equals(discoveryName)){
                    findIndex = i;
                    break;
                }
            }
            if(findIndex != -1){
                discoveries.remove(findIndex);
                config.set("categories."+categoryName+".discoveries",discoveries);
                commonConfig.saveConfig();
            }
        }
    }

    @Override
    public void createFiles() {

    }

}
