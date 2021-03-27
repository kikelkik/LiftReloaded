package com.minecraftcorp.lift.common.model;

import org.bukkit.entity.Player;

public enum Permission {

	RELOAD("lift.reload", "§cYou have no permission to reload the plugin"),
	CHANGE("lift.change", "§cYou have no permission to change the Lift sign");

	private final String node;
	private final String message;

	Permission(String node, String message) {
		this.node = node;
		this.message = message;
	}

	public static boolean hasPermission(Player player, Permission permission) {
		return player.hasPermission(permission.node);
	}

	public static void sendMessage(Player player, Permission permission) {
		player.sendMessage(permission.message);
	}
}
