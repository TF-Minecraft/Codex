package net.tfminecraft.codex.model.verify;

import net.tfminecraft.codex.api.CodexAPI;
import net.tfminecraft.codex.utils.JSONMessage;
import net.tfminecraft.codex.utils.JSONMessageAdventure;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CodexInventoryNotExistsError extends CodexBaseError {

    private String inventoryName;
    private String inventoryPath;
    private String slot;

    public CodexInventoryNotExistsError(String file, String errorText, boolean critical, String inventoryPath, String slot, String inventoryName) {
        super(file, errorText, critical);
        this.inventoryName = inventoryName;
        this.inventoryPath = inventoryPath;
        this.slot = slot;
    }

    @Override
    public void sendMessage(Player player) {
        List<String> hover = new ArrayList<String>();

        boolean isPaper = CodexAPI.getPlugin().getDependencyManager().isPaper();
        if(isPaper){
            JSONMessageAdventure jsonMessage = new JSONMessageAdventure(player,prefix+"<gray>Inventory <red>"+inventoryName+" <gray>not valid");
            hover.add("<yellow>THIS IS AN ERROR!");
            hover.add("<white>The <red>"+inventoryName+" <white>openened inventory used");
            hover.add("<white>on slot <red>"+slot+" <white>on inventory <red>"+inventoryPath);
            hover.add("<white>is not valid. Create it on the inventory.yml file.");

            jsonMessage.hover(hover).send();
        }else{
            JSONMessage jsonMessage = new JSONMessage(player,prefix+"&7Inventory &c"+inventoryName+" &7not valid");
            hover.add("&eTHIS IS AN ERROR!");
            hover.add("&fThe &c"+inventoryName+" &fopenened inventory used");
            hover.add("&fon slot &c"+slot+" &fon inventory &c"+inventoryPath);
            hover.add("&fis not valid. Create it on the inventory.yml file.");

            jsonMessage.hover(hover).send();
        }

    }
}
