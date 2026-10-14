package net.tfminecraft.codex.managers;

import net.tfminecraft.codex.Codex;
import net.tfminecraft.codex.model.data.PlayerData;
import net.tfminecraft.codex.model.data.PlayerDataCategory;
import net.tfminecraft.codex.model.data.PlayerDataDiscovery;
import net.tfminecraft.codex.model.internal.GenericCallback;
import net.tfminecraft.codex.model.structure.Category;
import net.tfminecraft.codex.model.structure.Discovery;
import net.tfminecraft.codex.utils.OtherUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayerDataManager {

    private Codex plugin;
    private Map<UUID, PlayerData> players;
    private Map<String,UUID> playerNames;

    public PlayerDataManager(Codex plugin){
        this.plugin = plugin;
        this.players = new HashMap<>();
        this.playerNames = new HashMap<>();
    }

    public Map<UUID,PlayerData> getPlayers() {
        return players;
    }

    public void addPlayer(PlayerData p){
        players.put(p.getUuid(),p);
        playerNames.put(p.getName(), p.getUuid());
    }

    /** The journal of the character the player is playing, or null between characters. */
    public PlayerData getPlayer(Player player){
        return players.get(player.getUniqueId());
    }

    public boolean isLoaded(Player player){
        return players.containsKey(player.getUniqueId());
    }

    public PlayerData getPlayerByUUID(UUID uuid){
        return players.get(uuid);
    }

    private UUID getPlayerUUID(String name){
        return playerNames.get(name);
    }

    public PlayerData getPlayerByName(String name){
        UUID uuid = getPlayerUUID(name);
        return players.get(uuid);
    }

    public void removePlayer(PlayerData playerData){
        players.remove(playerData.getUuid());
        playerNames.remove(playerData.getName());
    }

    public void removePlayerByUUID(UUID uuid){
        players.remove(uuid);
    }

    public void addDiscovery(Player player,String categoryName,String discoveryName){
        PlayerData playerData = getPlayer(player);
        if(playerData == null){
            return;
        }
        playerData.setModified(true);

        String date = OtherUtils.getDate(plugin.getConfigsManager().getMainConfigManager().getDiscoveriesDateFormat());
        playerData.addDiscovery(categoryName,discoveryName,date);
    }

    public boolean hasDiscovery(Player player,String categoryName,String discoveryName){
        PlayerData playerData = getPlayer(player);
        if(playerData == null){
            return false;
        }
        return playerData.hasDiscovery(categoryName,discoveryName);
    }

    public PlayerDataDiscovery getDiscovery(Player player,String categoryName,String discoveryName){
        PlayerData playerData = getPlayer(player);
        if(playerData == null){
            return null;
        }
        return playerData.getDiscovery(categoryName,discoveryName);
    }

    public int getTotalDiscoveries(Player player,String categoryName){
        PlayerData playerData = getPlayer(player);
        if(playerData == null){
            return 0;
        }
        return playerData.getTotalDiscoveries(categoryName);
    }

    public int getTotalDiscoveries(Player player){
        PlayerData playerData = getPlayer(player);
        if(playerData == null){
            return 0;
        }
        return playerData.getTotalDiscoveries();
    }

    public int getTotalDiscoveriesPercentage(Player player,String categoryName,int max){
        int totalDiscoveries = getTotalDiscoveries(player,categoryName);
        return OtherUtils.getPercentage(totalDiscoveries,max);
    }

    public int getTotalDiscoveriesPercentage(Player player,int max){
        int totalDiscoveries = getTotalDiscoveries(player);
        return OtherUtils.getPercentage(totalDiscoveries,max);
    }

    public void completeCategory(Player player,String categoryName){
        PlayerData playerData = getPlayer(player);
        if(playerData == null){
            return;
        }
        playerData.setModified(true);
        playerData.completeCategory(categoryName);
    }

    public boolean hasAllDiscoveries(Player player,String categoryName,int totalDiscoveries){
        PlayerData playerData = getPlayer(player);
        if(playerData == null){
            return false;
        }

        return playerData.getTotalDiscoveries(categoryName) >= totalDiscoveries;
    }

    public List<PlayerDataCategory> getCategories(Player player){
        PlayerData playerData = getPlayer(player);
        if(playerData == null){
            return new ArrayList<>();
        }

        return playerData.getCategories();
    }

    public boolean hasCompletedCategory(Player player,String categoryName){
        PlayerData playerData = getPlayer(player);
        if(playerData == null){
            return false;
        }
        return playerData.isCategoryCompleted(categoryName);
    }

    public boolean hasCompletedCategory(String playerName,String categoryName){
        PlayerData playerData = getPlayerByName(playerName);
        if(playerData == null){
            return false;
        }
        return playerData.isCategoryCompleted(categoryName);
    }

    public void setMillisActionsExecuted(Player player,String categoryName,String discoveryName){
        PlayerData playerData = getPlayer(player);
        if(playerData == null){
            return;
        }
        playerData.setModified(true);
        playerData.setMillisActionsExecuted(categoryName,discoveryName);
    }

    public long getMillisActionsExecuted(Player player,String categoryName,String discoveryName){
        PlayerData playerData = getPlayer(player);
        if(playerData == null){
            return 0;
        }

        return playerData.getMillisActionsExecuted(categoryName,discoveryName);
    }

    public String resetDataForPlayer(String playerName, String categoryName, String discoveryName, FileConfiguration messagesConfig){
        if(Bukkit.getPlayer(playerName) == null){
            return messagesConfig.getString("playerNotOnline");
        }

        PlayerData playerData = getPlayerByName(playerName);
        if(playerData == null){
            return messagesConfig.getString("playerDoesNotExists");
        }

        CategoryManager categoryManager = plugin.getCategoryManager();
        if(categoryName != null){
            Category category = categoryManager.getCategory(categoryName);
            if(category == null){
                return messagesConfig.getString("categoryDoesNotExist");
            }
            if(discoveryName != null && category.getDiscovery(discoveryName) == null){
                return messagesConfig.getString("discoveryDoesNotExist").replace("%category%",categoryName);
            }
        }

        if(categoryName == null){
            // Reset all data for player
            playerData.resetData();
            return messagesConfig.getString("commandResetAllForPlayer").replace("%player%",playerName);
        }

        if(discoveryName == null){
            // Reset category for player
            playerData.resetCategory(categoryName);
            return messagesConfig.getString("commandResetCategoryForPlayer").replace("%player%",playerName)
                    .replace("%category%",categoryName);
        }else{
            // Reset discovery for player
            playerData.resetDiscovery(categoryName,discoveryName);
            return messagesConfig.getString("commandResetDiscoveryForPlayer").replace("%player%",playerName)
                    .replace("%category%",categoryName).replace("%discovery%",discoveryName);
        }
    }

    public void resetDataForAllPlayers(String categoryName, String discoveryName, FileConfiguration messagesConfig, GenericCallback<String> callback){
        // Offline characters are reset in their files, played characters in memory and saved later.
        plugin.getConfigsManager().getPlayersConfigManager().resetDataForAllPlayers(categoryName,discoveryName);

        String result;
        if(categoryName == null){
            players.values().forEach(PlayerData::resetData);
            result = messagesConfig.getString("commandResetAllForAllPlayers");
        }else if(discoveryName == null){
            players.values().forEach(p -> p.resetCategory(categoryName));
            result = messagesConfig.getString("commandResetCategoryForAllPlayers").replace("%category%",categoryName);
        }else{
            players.values().forEach(p -> p.resetDiscovery(categoryName,discoveryName));
            result = messagesConfig.getString("commandResetDiscoveryForAllPlayers").replace("%category%",categoryName)
                    .replace("%discovery%",discoveryName);
        }
        callback.onDone(result);
    }
}
