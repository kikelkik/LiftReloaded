package com.minecraftcorp.lift.bukkit.command;

import com.minecraftcorp.lift.bukkit.LiftPlugin;
import com.minecraftcorp.lift.common.model.Permission;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class LiftCommand implements TabExecutor {

	private final LiftPlugin plugin = LiftPlugin.INSTANCE;

	public LiftCommand() {
		Objects.requireNonNull(plugin.getServer()
				.getPluginCommand("lift"))
				.setExecutor(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			return handlePlayerCommand(((Player) sender), args);
		}
		// command was send by console
		if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
			plugin.reload();
			return true;
		}
		return false;
	}

	private boolean handlePlayerCommand(Player player, String[] args) {
		if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
			if (!Permission.hasPermission(player, Permission.RELOAD)) {
				Permission.sendMessage(player, Permission.RELOAD);
				return false;
			}
			plugin.reload();
			player.sendMessage("Lift successfully reloaded");
			return true;
		}
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if ("reload".startsWith(args[0]) && args.length == 1) {
			return Collections.singletonList("reload");
		}
		return Collections.emptyList();
	}
}
