package pl.dsocraft.auctionhouse.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.dsocraft.auctionhouse.DSOAuctionHouse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles tab completion for auction house commands.
 */
public class AuctionTabCompleter implements TabCompleter {

    private final DSOAuctionHouse plugin;

    public AuctionTabCompleter(DSOAuctionHouse plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("ah")) {
            if (args.length == 1) {
                // First argument for /ah
                List<String> subcommands = Arrays.asList("find", "mailbox", "help");
                return subcommands.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args.length == 2 && args[0].equalsIgnoreCase("find")) {
                // No suggestions for /ah find as requested
                return new ArrayList<>();
            }
        } else if (command.getName().equalsIgnoreCase("checkah")) {
            if (args.length == 1) {
                // Player names for /checkah
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (command.getName().equalsIgnoreCase("sell")) {
            if (args.length == 1) {
                // Price suggestions for /sell
                return Arrays.asList("100", "1k", "10k", "100k", "1m", "10m", "100m", "1b");
            }
        } else if (command.getName().equalsIgnoreCase("ahadmin")) {
            if (args.length == 1) {
                // Admin subcommands
                return Arrays.asList("reload", "help").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }
}
