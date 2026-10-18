package net.tfminecraft.codex.managers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.tfminecraft.codex.Codex;
import net.tfminecraft.codex.character.Exploration;
import net.tfminecraft.codex.character.SharedExploration;
import net.tfminecraft.codex.config.MessagesConfigManager;
import net.tfminecraft.codex.model.internal.CommonVariable;
import net.tfminecraft.codex.model.structure.Category;
import net.tfminecraft.codex.model.structure.Discovery;
import net.tfminecraft.codex.utils.ActionUtils;

/**
 * Codex's side of shared discoveries: the {@code shared_discoveries} settings, and the reward lines a
 * copied discovery carries over. Messages, titles and sounds are left out; one summary message is sent instead.
 */
public class ExplorationManager implements Exploration {
    private final Codex plugin;

    public ExplorationManager(Codex plugin) {
        this.plugin = plugin;
    }

    @Override
    public SharedExploration.Settings settings() {
        return plugin.getConfigsManager().getMainConfigManager().getSharedExploration();
    }

    @Override
    public void reward(Player player, List<SharedExploration.Copy> copies) {
        SharedExploration.Settings settings = settings();
        CategoryManager categories = plugin.getCategoryManager();
        PlayerDataManager players = plugin.getPlayerDataManager();
        Set<Category> touched = new LinkedHashSet<>();
        for (SharedExploration.Copy copy : copies) {
            Category category = categories.getCategory(copy.category());
            Discovery discovery = category == null ? null : category.getDiscovery(copy.discovery());
            if (discovery == null) {
                continue;
            }
            touched.add(category);
            List<String> rewards = discovery.getCustomRewards() != null
                    ? discovery.getCustomRewards() : category.getDefaultRewardsPerDiscovery();
            run(player, rewards, settings, discovery.getName());
        }
        // As when a discovery is found: completing a category pays its completion rewards once.
        for (Category category : touched) {
            String name = category.getName();
            if (players.hasAllDiscoveries(player, name, category.getDiscoveries().size())
                    && !players.hasCompletedCategory(player, name)) {
                players.completeCategory(player, name);
                run(player, category.getDefaultRewardsAllDiscoveries(), settings, name);
            }
        }
        String message = plugin.getMessagesConfig().getString("sharedDiscoveries", MessagesConfigManager.SHARED_DISCOVERIES);
        if (message != null && !message.isEmpty()) {
            plugin.getMessagesManager().sendMessage(player, message.replace("%amount%", String.valueOf(copies.size())), true);
        }
    }

    private void run(Player player, List<String> rewards, SharedExploration.Settings settings, String name) {
        if (rewards == null) {
            return;
        }
        ArrayList<CommonVariable> variables = new ArrayList<>();
        variables.add(new CommonVariable("%name%", name));
        for (String line : rewards) {
            String carried = settings.reward(line);
            if (carried != null) {
                ActionUtils.executeAction(player, carried, plugin, variables);
            }
        }
    }

    @Override
    public void later(long ticks, Runnable task) {
        Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
    }
}
