package com.keikun1215.fuwautils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.keikun1215.fuwautils.util.InventoryUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class FuwaUtilCommands implements CommandExecutor {
    private final String prefix = "§7[§dFuwaUtils§7] ";
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("managers")) {
            if (args.length == 0) {
                commandSender.sendMessage(
                        prefix + "Managers: " + Fuwautils.MANAGERS
                );
                return true;
            }
            if (Objects.equals(args[0], "death")) {
                if (Objects.equals(args[1], "list")) {
                    if (!commandSender.isOp()) return e(commandSender, "No permission");
                    if (args.length == 2) return e(commandSender, "Must include selector.\n/managers death list <name>");
                    JsonObject dieData = getDieData();
                    Player player = Fuwautils.INSTANCE.getServer().getPlayer(args[2]);
                    if (player != null) {
                        JsonArray deathes = dieData.getAsJsonObject("deathes").getAsJsonArray(player.getUniqueId().toString());
                        if (deathes == null || deathes.size() == 0) return e(commandSender, "No dies found.");
                        int i = 0;
                        for (JsonElement death : deathes) {
                            JsonObject deathData = (JsonObject) death;
                            TextComponent component = new TextComponent("§7<§e補填§7>");
                            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/managers death reqcom " + player.getUniqueId() + " " + i));
                            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("補填")));
                            TextComponent message = new TextComponent("§7[MSG] ");
                            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(deathData.get("message").getAsString())));
                            TextComponent components = new TextComponent("§f[§b"
                                    + deathData.get("time").getAsString()
                                    + " §f@§b"
                                    + deathData.get("location").getAsString()
                                    + "§f] ");
                            components.addExtra(message);
                            components.addExtra(component);
                            commandSender.spigot().sendMessage(components);
                            i++;
                        }
                    } else return e(commandSender, "The specified player " + args[2] + " not found.");
                } else if (Objects.equals(args[1], "compense")) {
                    if (args.length == 2 || args.length == 3) return e(commandSender, "Must include death number.\n/managers death compense <player> <int>");
                    JsonObject dieData = getDieData();
                    Player player = Fuwautils.INSTANCE.getServer().getPlayer(UUID.fromString(args[2]));
                    if (!player.getScoreboardTags().contains("reqcom")) return e(player, "既に終了した処理です。");
                    player.removeScoreboardTag("reqcom");
                    int index = Integer.parseInt(args[3]);
                    JsonArray deathes = dieData.getAsJsonObject("deathes").getAsJsonArray(args[2]);
                    if (deathes == null || deathes.get(index) == null) {
                        commandSender.sendMessage("§9 No dies found.");
                        return false;
                    }
                    JsonArray items = deathes.get(index).getAsJsonObject().get("items").getAsJsonArray();
                    try {
                        player.sendMessage(prefix + "§f死亡§7" + deathes.get(index).getAsJsonObject().get("time").getAsString() + "§fのアイテムが補填されました");
                        player.getInventory().setContents(
                                InventoryUtils.itemStackArrayFromBase64(items.get(0).getAsString())
                        );
                        player.getInventory().setArmorContents(
                                InventoryUtils.itemStackArrayFromBase64(items.get(1).getAsString())
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (Objects.equals(args[1], "reqcom")) {
                    if (args.length == 2 || args.length == 3) return e(commandSender, "Must include death number.\n/managers death reqcom <player> <int>");
                    Player player = Fuwautils.INSTANCE.getServer().getPlayer(UUID.fromString(args[2]));
                    player.addScoreboardTag("reqcom");
                    TextComponent component = new TextComponent("§b補填が来ています。何らかのアイテムを持ったままこれを受け入れると§c§lアイテムが消失§r§bします。インベントリを空にしてから承認してください。\n");
                    TextComponent accept = new TextComponent("§a[承認]");
                    accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Accept")));
                    accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/managers death compense " + args[2] + " " + args[3]));
                    TextComponent deny = new TextComponent("§c[拒否]");
                    deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Deny")));
                    deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/managers death dencom " + commandSender.getName()));
                    component.addExtra(accept);
                    component.addExtra(" ");
                    component.addExtra(deny);
                    player.spigot().sendMessage(component);
                } else if (Objects.equals(args[1], "dencom")) {
                    Player sender = Fuwautils.INSTANCE.getServer().getPlayer(args[2]);
                    if (commandSender instanceof Player) {
                        Player senderp = (Player) commandSender;
                        if (senderp.getScoreboardTags().contains("reqcom")) sender.sendMessage("§c拒否されました。");
                        else return e(sender, "既に終了した処理です。");
                        senderp.removeScoreboardTag("reqcom");
                    }
                }
            }
        }
        return true;
    }
    private boolean e(CommandSender c, String message) {
        c.sendMessage("§c[ERR] " + message);
        return false;
    }
    private JsonObject getDieData() {
        File deathFile = new File(Fuwautils.INSTANCE.getDataFolder(), "dieData.json");
        JsonReader reader = null;
        try {
            reader = new JsonReader(new FileReader(deathFile));
        } catch (FileNotFoundException ignored) {}
        return (JsonObject) Streams.parse(reader);
    }
}
