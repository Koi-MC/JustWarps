package koi.pond.justWarps;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WarpTabCompleter implements TabCompleter {
    private final JustWarps plugin;

    public WarpTabCompleter(JustWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("warp")) {
            if (args.length == 1) {
                // Get the set of available warps
                ConfigurationSection section = plugin.getWarpsConfig().getConfigurationSection("warps");
                if (section != null) {
                    Set<String> warpNames = section.getKeys(false);
                    return new ArrayList<>(warpNames);
                } else {
                    // Return an empty list if the section is null
                    return new ArrayList<>();
                }
            }
        }
        return null; // Return null for no suggestions
    }
}
