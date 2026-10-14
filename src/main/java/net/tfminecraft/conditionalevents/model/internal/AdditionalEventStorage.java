package net.tfminecraft.conditionalevents.model.internal;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AdditionalEventStorage {
    private Map<String,ItemStack> items;

    public AdditionalEventStorage() {
        this.items = new HashMap<>();
    }

    public void setItem(String tag, ItemStack item) {
        if(tag == null){
            items.put("normal",item);
        }else{
            items.put(tag,item);
        }
    }

    public ItemStack getStoredItem(String tag){
        return items.get(tag);
    }
}
