package net.tfminecraft.codex.listeners;

import net.tfminecraft.codex.Codex;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private Codex plugin;
    public PlayerListener(Codex plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        plugin.getPlayerDataManager().manageJoin(event.getPlayer());
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event){
        LivingEntity e = event.getEntity();
        if(e.getKiller() != null){
            String customName = e.getCustomName() != null ? ChatColor.stripColor(e.getCustomName()) : null;
            plugin.getDiscoveryManager().onMobKill(e.getKiller(),e.getType().name(),customName);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event){
        plugin.getPlayerDataManager().manageLeave(event.getPlayer());
    }
}
