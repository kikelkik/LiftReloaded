package io.liftreloaded.spigot.listener;

import io.liftreloaded.bukkit.listener.PlayerListener;
import org.bukkit.event.EventHandler;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public class SpigotPlayerListener extends PlayerListener {

	@EventHandler(ignoreCancelled = true)
	public void onLogin(PlayerSpawnLocationEvent event) {
		handleLogin(event);
	}
}
