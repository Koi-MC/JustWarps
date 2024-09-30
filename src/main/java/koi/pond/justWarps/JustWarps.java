package koi.pond.justWarps;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.ChatColor;
import org.bukkit.Sound;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class JustWarps extends JavaPlugin {

    private File warpsFile;
    private FileConfiguration warpsConfig;

    private Map<UUID, Long> cooldowns = new HashMap<>();
    private int cooldownTime;
    private boolean throughDimension;

    public FileConfiguration getWarpsConfig() {
        return warpsConfig;
    }

    double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void createConfigFile() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getConfig().options().copyDefaults(true);
            getConfig().addDefault("cooldown", 10);
            getConfig().addDefault("through_dimension", false);
            saveConfig();
        }
    }

    // Save warps.yml
    public void saveWarpsFile() {
        File warpsFile = new File(getDataFolder(), "warps.yml");
        try {
            warpsConfig.save(warpsFile); // Ensure you're passing the correct file reference
            getLogger().info("Saved warps to warps.yml.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Create or load warps.yml
    private void createWarpsFile() {
        File warpsFile = new File(getDataFolder(), "warps.yml");
        if (!warpsFile.exists()) {
            try {
                if (warpsFile.getParentFile() != null) {
                    warpsFile.getParentFile().mkdirs(); // Create directories if needed
                }
                warpsFile.createNewFile(); // Create the warps.yml file
                getLogger().info("Created new warps.yml file.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);
        // Make sure to create the "warps" section if it doesn't exist
        if (!warpsConfig.contains("warps")) {
            warpsConfig.createSection("warps");
            saveWarpsFile();
        }
    }

    @Override
    public void onEnable() {
        createConfigFile();
        createWarpsFile();
        this.getCommand("warp").setTabCompleter(new WarpTabCompleter(this));
        cooldownTime = getConfig().getInt("cooldown");
        throughDimension = getConfig().getBoolean("through_dimension");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("setwarp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can set warps.");
                return true;
            }

            Player player = (Player) sender;
            if (args.length != 1) {
                player.sendMessage("Usage: /setwarp <name>");
                return true;
            }

            String warpName = args[0];
            Location location = player.getLocation();

            // Store the world (dimension) along with the warp coordinates
            warpsConfig.set("warps." + warpName + ".world", location.getWorld().getName());
            warpsConfig.set("warps." + warpName + ".x", roundToTwoDecimals(location.getX()));
            warpsConfig.set("warps." + warpName + ".y", roundToTwoDecimals(location.getY()));
            warpsConfig.set("warps." + warpName + ".z", roundToTwoDecimals(location.getZ()));
            warpsConfig.set("warps." + warpName + ".yaw", (float) roundToTwoDecimals(location.getYaw()));
            warpsConfig.set("warps." + warpName + ".pitch", (float) roundToTwoDecimals(location.getPitch()));

            saveWarpsFile();
            player.sendMessage(ChatColor.GREEN + "Warp point '" + warpName + "' set.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("warp")) {
            if (args.length == 0) {
                Bukkit.getScheduler().runTask(this, () -> {
                    sender.sendMessage(ChatColor.RED + "Usage: /warp <name>");
                    sender.sendMessage("Use /warplist to see available warps.");
                });
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }

            Player player = (Player) sender;
            String warpName = args[0];

            // Check for cooldown
            if (cooldowns.containsKey(player.getUniqueId())) {
                long secondsLeft = (cooldowns.get(player.getUniqueId()) + (cooldownTime * 1000) - System.currentTimeMillis()) / 1000;
                if (secondsLeft > 0) {
                    player.sendMessage(ChatColor.RED + "You must wait " + secondsLeft + " seconds before warping again.");
                    return true;
                }
            }

            // Proceed with the warp
            if (warpsConfig.contains("warps." + warpName + ".world") &&
                    warpsConfig.contains("warps." + warpName + ".x") &&
                    warpsConfig.contains("warps." + warpName + ".y") &&
                    warpsConfig.contains("warps." + warpName + ".z") &&
                    warpsConfig.contains("warps." + warpName + ".yaw") &&
                    warpsConfig.contains("warps." + warpName + ".pitch")) {

                String worldName = warpsConfig.getString("warps." + warpName + ".world");
                double x = warpsConfig.getDouble("warps." + warpName + ".x");
                double y = warpsConfig.getDouble("warps." + warpName + ".y");
                double z = warpsConfig.getDouble("warps." + warpName + ".z");
                float yaw = (float) warpsConfig.getDouble("warps." + warpName + ".yaw");
                float pitch = (float) warpsConfig.getDouble("warps." + warpName + ".pitch");
                Location location = new Location(getServer().getWorld(worldName), x, y, z, yaw, pitch);

                // Check if the player is in the same dimension
                //if (!player.getWorld().getName().equals(worldName)) {
                if (!throughDimension && !player.getWorld().getName().equals(worldName)) {
                    player.sendMessage(ChatColor.RED + "You can only warp to locations in the same dimension.");
                    return true;
                }

                // Teleport the player
                player.teleport(location);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f); // Play tele sound

                // Set cooldown for the player
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

                player.sendMessage(ChatColor.AQUA + "Warped to '" + warpName + "'");
            } else {
                player.sendMessage(ChatColor.RED + "Warp '" + warpName + "' does not exist.");
            }
            return true;
        }

        // List warp points with /warplist or /listwarp
        if (command.getName().equalsIgnoreCase("warplist") || command.getName().equalsIgnoreCase("listwarp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can list warps.");
                return true;
            }

            Player player = (Player) sender;
            if (!warpsConfig.contains("warps")) {
                player.sendMessage("No warp points have been set.");
                return true;
            }

            // Get all the warp names
            Set<String> warpNamesSet = warpsConfig.getConfigurationSection("warps").getKeys(false);
            if (warpNamesSet.isEmpty()) {
                player.sendMessage("No warp points have been set.");
            } else {
                // Convert to List, sort it, and join into a single string
                ArrayList<String> warpNamesList = new ArrayList<>(warpNamesSet);
                Collections.sort(warpNamesList);

                StringBuilder warpListString = new StringBuilder("");
                for (int i = 0; i < warpNamesList.size(); i++) {
                    if (i > 0) {
                        warpListString.append(", "); // Append ", " before subsequent warp names
                    }
                    warpListString.append(warpNamesList.get(i)); // Append the warp name
                }
                Bukkit.getScheduler().runTask(this, () -> {
                    player.sendMessage("Available warp points: ");
                    player.sendMessage(ChatColor.GOLD + warpListString.toString()); // Send the full list to the player
                });
            }
            return true;
        }

        // Command to delete a warp point: /delwarp <warp name>
        if (command.getName().equalsIgnoreCase("delwarp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can delete warps.");
                return true;
            }

            Player player = (Player) sender;
            if (args.length != 1) {
                player.sendMessage("Usage: /delwarp <name>");
                return true;
            }

            String warpName = args[0];
            if (!warpsConfig.contains("warps." + warpName)) {
                player.sendMessage("Warp point '" + warpName + "' not found.");
                return true;
            }

            // Remove the warp from the configuration
            warpsConfig.set("warps." + warpName, null);
            saveWarpsFile();  // Save changes to warps.yml
            player.sendMessage("Warp point '" + warpName + "' has been deleted.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("warpconfigreload") || command.getName().equalsIgnoreCase("reloadwarpconfig")) {
            reloadConfig(); // Reloads the config.yml
            cooldownTime = getConfig().getInt("cooldown"); // Reload cooldown time
            throughDimension = getConfig().getBoolean("through_dimension"); // Reload through_dimension

            // Send the message in the next tick to ensure it's in order
            Bukkit.getScheduler().runTask(this, () -> {
                sender.sendMessage(ChatColor.GREEN + "Warp configuration reloaded successfully");
            });

            return true; // Indicate the command was handled
        }

        return false;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
    }
}
