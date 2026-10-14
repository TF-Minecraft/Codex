package net.tfminecraft.codex.model.verify;

import net.tfminecraft.codex.api.CodexAPI;
import net.tfminecraft.codex.utils.JSONMessage;
import net.tfminecraft.codex.utils.JSONMessageAdventure;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CodexInventoryDefaultNotExistsError extends CodexBaseError {

    private String inventoryName;

    public CodexInventoryDefaultNotExistsError(String file, String errorText, boolean critical, String inventoryName) {
        super(file, errorText, critical);
        this.inventoryName = inventoryName;
    }

    @Override
    public void sendMessage(Player player) {
        List<String> hover = new ArrayList<String>();

        boolean isPaper = CodexAPI.getPlugin().getDependencyManager().isPaper();
        if(isPaper){
            JSONMessageAdventure jsonMessage = new JSONMessageAdventure(player,prefix+"<gray>Inventory <red>"+inventoryName+" <gray>not found");
            hover.add("<yellow>THIS IS AN ERROR!");
            hover.add("<white>The <red>"+inventoryName+" <white>is a needed inventory for");
            hover.add("<white>the plugin to work. You MUST NOT delete it");
            hover.add("<white>from the inventory.yml file. You can find the default");
            hover.add("<white>config for this inventory on the wiki.");

            jsonMessage.hover(hover).send();
        }else{
            JSONMessage jsonMessage = new JSONMessage(player,prefix+"&7Inventory &c"+inventoryName+" &7not found");
            hover.add("&eTHIS IS AN ERROR!");
            hover.add("&fThe &c"+inventoryName+" &fis a needed inventory for");
            hover.add("&fthe plugin to work. You MUST NOT delete it");
            hover.add("&ffrom the inventory.yml file. You can find the default");
            hover.add("&fconfig for this inventory on the wiki.");

            jsonMessage.hover(hover).send();
        }

    }
}
