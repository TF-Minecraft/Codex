package net.tfminecraft.codex.model.verify;

import net.tfminecraft.codex.api.CodexAPI;
import net.tfminecraft.codex.utils.JSONMessage;
import net.tfminecraft.codex.utils.JSONMessageAdventure;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CodexInventoryInvalidDiscoveryError extends CodexBaseError {

    private String levelName;
    private String categoryName;
    private String inventoryName;
    private String slot;

    public CodexInventoryInvalidDiscoveryError(String file, String errorText, boolean critical, String levelName, String categoryName, String inventoryName, String slot) {
        super(file, errorText, critical);
        this.levelName = levelName;
        this.categoryName = categoryName;
        this.inventoryName = inventoryName;
        this.slot = slot;
    }

    @Override
    public void sendMessage(Player player) {
        List<String> hover = new ArrayList<String>();

        boolean isPaper = CodexAPI.getPlugin().getDependencyManager().isPaper();
        if(isPaper){
            JSONMessageAdventure jsonMessage = new JSONMessageAdventure(player,prefix+"<gray>Invalid discovery named <red>"+levelName+" <gray>on file <red>"+file);
            hover.add("<yellow>THIS IS AN ERROR!");
            hover.add("<white>A non existing discovery associated with category ");
            hover.add("<red>"+categoryName+" <white>is present on <white>inventory");
            hover.add("<red>"+inventoryName+" <white>and slot <red>"+slot+"<white>.");

            jsonMessage.hover(hover).send();
        }else{
            JSONMessage jsonMessage = new JSONMessage(player,prefix+"&7Invalid discovery named &c"+levelName+" &7on file &c"+file);
            hover.add("&eTHIS IS AN ERROR!");
            hover.add("&fA non existing discovery associated with category ");
            hover.add("&c"+categoryName+" &fis present on &finventory");
            hover.add("&c"+inventoryName+" &fand slot &c"+slot+"&f.");

            jsonMessage.hover(hover).send();
        }

    }
}
