package net.tfminecraft.codex.listeners.dependencies;

import net.tfminecraft.codex.Codex;
import net.tfminecraft.codex.managers.dependencies.worldguard.RegionEnteredEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class WorldGuardListener implements Listener {

    private Codex plugin;
    public WorldGuardListener(Codex plugin){
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldGuardRegionEnter(RegionEnteredEvent event){
        if(event.isCancelled()){
            return;
        }
        plugin.getDiscoveryManager().onWorldGuardRegionEnter(event.getPlayer(),event.getRegionName());
    }
}
