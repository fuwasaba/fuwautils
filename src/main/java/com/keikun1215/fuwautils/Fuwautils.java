package com.keikun1215.fuwautils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.keikun1215.fuwautils.death.DeathInfo;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class Fuwautils extends JavaPlugin implements Listener, TabCompleter {
    public static Fuwautils INSTANCE;
    public static final Logger LOGGER = PluginLogger.getLogger("Fuwautils");
    public static final List<String> MANAGERS = Arrays.asList(
            "death"
    );
    @Override
    public void onEnable() {
        LOGGER.info("Data directory initialization");
        getDataFolder().mkdirs();
        INSTANCE = this;
        LOGGER.info("Die data directory initialization");
        makeAndDefaultData("dieData.json", "{\"deathes\":{}}");
        LOGGER.info("Events initialization");
        getServer().getPluginManager().registerEvents(this, this);
        PluginCommand command = getCommand("managers");
        if (command != null) {
            command.setExecutor(new FuwaUtilCommands());
            command.setTabCompleter(this);
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("managers")) {
            if (args.length == 1) {
                return MANAGERS;
            }
            if (args.length == 2 && Objects.equals(args[0], "death")) {
                List<String> res = new ArrayList<>();
                res.add("list");
                return res;
            }
            if (args.length == 4 && Objects.equals(args[2], "list")) return players();
            return Collections.emptyList();
        }
        return null;
    }
    public List<String> players() {
        return getServer().getOnlinePlayers().stream().map(Player::getDisplayName).collect(Collectors.toList());
    }
    public static void makeAndDefaultData(String name, @Nullable String defau1t) {
        File file = new File(INSTANCE.getDataFolder(), name);
        if (!file.exists()) {
            try {
                file.createNewFile();
                if (defau1t != null) {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    writer.write(defau1t);
                    writer.newLine();
                    writer.close();
                }
            } catch (IOException ignored) {}
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) throws IOException {
        Player player = event.getEntity();
        DeathInfo info = new DeathInfo(player, event.getDeathMessage());
        event.setDeathMessage(event.getDeathMessage() + " §8*");
        File data = new File(getDataFolder(), "dieData.json");
        BufferedReader reader = new BufferedReader(new FileReader(data));
        StringBuilder file = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            file.append(line).append("\n");
        }
        reader.close();
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(file.toString(), JsonObject.class);

        JsonObject obj = json.getAsJsonObject("deathes");
        JsonArray dies = obj.getAsJsonArray(info.name.toString());
        if (dies == null) {
            obj.add(info.name.toString(), new JsonArray());
            obj.getAsJsonArray(info.name.toString()).add(info.toJson());
        } else dies.add(info.toJson());
        json.remove("deathes");
        json.add("deathes", obj);

        BufferedWriter writer = new BufferedWriter(new FileWriter(data));
        for (String aLine : JsonUtils.prettyPrint(json.toString()).split(System.lineSeparator())) {
            writer.write(aLine);
            writer.newLine();
        }
        writer.close();
    }
}
