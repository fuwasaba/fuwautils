package com.keikun1215.fuwautils.death;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.keikun1215.fuwautils.util.InventoryUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class DeathInfo {
    public String time;
    public UUID name;
    public PlayerInventory inventory;
    public Location location;
    public String message;
    public DeathInfo(Player player, String message) {
        this.inventory = player.getInventory();
        this.time = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS").format(LocalDateTime.now());
        this.name = player.getUniqueId();
        this.location = player.getLocation();
        this.message = message;
    }
    public JsonObject toJson() {
        JsonObject result = new JsonObject();
        result.add("time", new JsonPrimitive(time));
        result.add("location", new JsonPrimitive("x:" + location.getBlockX() + " y:" + location.getBlockY() + " z:" + location.getBlockZ()));
        result.add("message", new JsonPrimitive(message));
        JsonArray items = new JsonArray();
        items.add(new JsonPrimitive(InventoryUtils.itemStackArrayToBase64(inventory.getContents())));
        items.add(new JsonPrimitive(InventoryUtils.itemStackArrayToBase64(inventory.getArmorContents())));
        result.add("items", items);
        return result;
    }
}
