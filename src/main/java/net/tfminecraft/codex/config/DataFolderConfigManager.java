package net.tfminecraft.codex.config;


import net.tfminecraft.codex.Codex;

import java.io.File;
import java.util.ArrayList;

public abstract class DataFolderConfigManager {

    protected Codex plugin;
    protected String folderName;

    public DataFolderConfigManager(Codex plugin, String folderName) {
        this.plugin = plugin;
        this.folderName = folderName;
    }

    public void configure() {
        createFolder();
        loadConfigs();
    }

    public void createFolder(){
        File folder;
        try {
            folder = new File(plugin.getDataFolder() + File.separator + folderName);
            if(!folder.exists()){
                folder.mkdirs();
                createFiles();
            }
        } catch(SecurityException e) {
            folder = null;
        }
    }

    public CommonConfig getConfigFile(String pathName,boolean create) {
        String pathFile = plugin.getDataFolder() + File.separator + folderName;
        File folder = new File(pathFile);
        File file = new File(folder, pathName);
        if(!file.exists() && !create) {
            return null;
        }

        CommonConfig commonConfig = new CommonConfig(pathName, plugin, folderName, true);
        commonConfig.registerConfig();
        return commonConfig;
    }

    public ArrayList<CommonConfig> getConfigs(){
        return getConfigs(folderName);
    }

    /** Every file in a folder of the data folder; none if the folder is missing. */
    public ArrayList<CommonConfig> getConfigs(String folder){
        ArrayList<CommonConfig> configs = new ArrayList<>();

        File[] listOfFiles = new File(plugin.getDataFolder() + File.separator + folder).listFiles();
        if(listOfFiles == null){
            return configs;
        }
        for (File file : listOfFiles) {
            if (file.isFile()) {
                String pathName = file.getName();
                CommonConfig commonConfig = new CommonConfig(pathName, plugin, folder, true);
                commonConfig.registerConfig();
                configs.add(commonConfig);
            }
        }

        return configs;
    }

    public abstract void createFiles();

    public abstract void loadConfigs();

    public abstract void saveConfigs();
}
