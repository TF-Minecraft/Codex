package net.tfminecraft.codex;

import net.tfminecraft.codex.api.CodexAPI;
import net.tfminecraft.codex.character.CharacterJournals;
import net.tfminecraft.codex.character.CharacterStore;
import net.tfminecraft.codex.api.ExpansionCodex;
import net.tfminecraft.codex.commands.MainCommand;
import net.tfminecraft.codex.config.ConfigsManager;
import net.tfminecraft.codex.listeners.InventoryListener;
import net.tfminecraft.codex.listeners.PlayerListener;
import net.tfminecraft.codex.listeners.dependencies.EliteMobsListener;
import net.tfminecraft.codex.listeners.dependencies.MythicMobsListener;
import net.tfminecraft.codex.listeners.dependencies.ResidenceListener;
import net.tfminecraft.codex.listeners.dependencies.WorldGuardListener;
import net.tfminecraft.codex.managers.*;
import net.tfminecraft.codex.managers.dependencies.DependencyManager;
import net.tfminecraft.codex.tasks.PlayerDataSaveTask;
import net.tfminecraft.codex.utils.ServerVersion;
import net.tfminecraft.codex.versions.NMSManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Codex extends JavaPlugin {
    PluginDescriptionFile pdfFile = getDescription();
    public String version = pdfFile.getVersion();
    public static String prefix;
    public static ServerVersion serverVersion;

    private ConfigsManager configsManager;
    private NMSManager nmsManager;
    private CommonItemManager commonItemManager;
    private MessagesManager messagesManager;
    private InventoryManager inventoryManager;
    private CategoryManager categoryManager;
    private DiscoveryManager discoveryManager;
    private DependencyManager dependencyManager;
    private PlayerDataManager playerDataManager;
    private CharacterJournals characterJournals;

    private PlayerDataSaveTask playerDataSaveTask;
    private VerifyManager verifyManager;

    public void onEnable(){
        setVersion();
        setPrefix();
        this.playerDataManager = new PlayerDataManager(this);
        this.inventoryManager = new InventoryManager(this);
        this.commonItemManager = new CommonItemManager(this);
        this.categoryManager = new CategoryManager(this);
        this.discoveryManager = new DiscoveryManager(this);
        this.dependencyManager = new DependencyManager(this);
        this.nmsManager = new NMSManager(this);
        this.configsManager = new ConfigsManager(this);
        this.configsManager.configure();

        if(configsManager.getMainConfigManager().getConfigVersion() != 2){
            legacyVersionError();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        characterJournals = new CharacterJournals(playerDataManager, new CharacterStore(getDataFolder().toPath()), getLogger());
        registerEvents();
        registerCommands();
        characterJournals.start();

        reloadPlayerDataSaveTask();

        CodexAPI api = new CodexAPI(this);
        if(getServer().getPluginManager().getPlugin("PlaceholderAPI") != null){
            new ExpansionCodex(this).register();
        }

        this.verifyManager = new VerifyManager(this);
        this.verifyManager.verify();

        Bukkit.getConsoleSender().sendMessage(prefix+MessagesManager.getLegacyColoredMessage("&eHas been enabled! &fVersion: "+version));
    }

    public void onDisable(){
        if(characterJournals != null){
            characterJournals.stop();
        }

        Bukkit.getConsoleSender().sendMessage(prefix+MessagesManager.getLegacyColoredMessage("&eHas been disabled! &fVersion: "+version));
    }
    public void registerCommands(){
        this.getCommand("codex").setExecutor(new MainCommand(this));
    }

    public void registerEvents(){
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new InventoryListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(characterJournals, this);
        if(dependencyManager.getWorldGuardManager() != null){
            pm.registerEvents(new WorldGuardListener(this), this);
        }
        if(dependencyManager.isMythicMobs()){
            pm.registerEvents(new MythicMobsListener(this), this);
        }

        if(dependencyManager.isEliteMobs()){
            pm.registerEvents(new EliteMobsListener(this), this);
        }

        if(dependencyManager.isResidence()){
            pm.registerEvents(new ResidenceListener(this), this);
        }

    }

    public void setPrefix(){
        prefix = MessagesManager.getLegacyColoredMessage("&4[&cCodex&4] ");
    }

    public void setVersion(){
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String minecraftVersion = ServerVersion.getMinecraftVersion();
        switch(minecraftVersion){
            case "1.20.5":
            case "1.20.6":
                serverVersion = ServerVersion.v1_20_R4;
                break;
            case "1.21":
            case "1.21.1":
                serverVersion = ServerVersion.v1_21_R1;
                break;
            case "1.21.2":
            case "1.21.3":
                serverVersion = ServerVersion.v1_21_R2;
                break;
            case "1.21.4":
                serverVersion = ServerVersion.v1_21_R3;
                break;
            case "1.21.5":
                serverVersion = ServerVersion.v1_21_R4;
                break;
            case "1.21.6":
            case "1.21.7":
            case "1.21.8":
                serverVersion = ServerVersion.v1_21_R5;
                break;
            case "1.21.9":
            case "1.21.10":
                serverVersion = ServerVersion.v1_21_R6;
                break;
            case "1.21.11":
                serverVersion = ServerVersion.v1_21_R7;
                break;
            case "26.1":
            case "26.1.1":
            case "26.1.2":
                serverVersion = ServerVersion.v26_1;
                break;
            case "26.2":
                serverVersion = ServerVersion.v26_2;
                break;
            case "26.3":
                serverVersion = ServerVersion.v26_3;
                break;
            default:
                try{
                    serverVersion = ServerVersion.valueOf(packageName.replace("org.bukkit.craftbukkit.", ""));
                }catch(Exception e){
                    serverVersion = ServerVersion.v26_3;
                }
        }
    }

    public void reloadPlayerDataSaveTask() {
        if(playerDataSaveTask != null) {
            playerDataSaveTask.end();
        }
        playerDataSaveTask = new PlayerDataSaveTask(this);
        playerDataSaveTask.start(configsManager.getMainConfigManager().getPlayerDataSave());
    }

    public CommonItemManager getCommonItemManager() {
        return commonItemManager;
    }

    public ConfigsManager getConfigsManager() {
        return configsManager;
    }

    public NMSManager getNmsManager() {
        return nmsManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    public void setMessagesManager(MessagesManager messagesManager) {
        this.messagesManager = messagesManager;
    }

    public FileConfiguration getMessagesConfig(){
        return configsManager.getMessagesConfigManager().getConfigFile().getConfig();
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public DependencyManager getDependencyManager() {
        return dependencyManager;
    }

    public CharacterJournals getCharacterJournals() {
        return characterJournals;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public CategoryManager getCategoryManager() {
        return categoryManager;
    }

    public DiscoveryManager getDiscoveryManager() {
        return discoveryManager;
    }

    public VerifyManager getVerifyManager() {
        return verifyManager;
    }

    public void legacyVersionError(){
        Bukkit.getConsoleSender().sendMessage(prefix+MessagesManager.getLegacyColoredMessage("&cERROR ENABLING THE PLUGIN!"));
        Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage("&cYour current config is not supported by the newer"));
        Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage("&cversions of the plugin. More information on the"));
        Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage("&cfollowing link: &fhttps://www.spigotmc.org/resources/90371/"));
    }
}
