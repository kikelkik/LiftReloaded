package com.minecraftcorp.lift.bukkit.command;

import java.util.Objects;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.minecraftcorp.lift.bukkit.LiftPlugin;
import com.minecraftcorp.lift.common.model.Permission;

public class LiftCommand implements CommandExecutor {

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
}
