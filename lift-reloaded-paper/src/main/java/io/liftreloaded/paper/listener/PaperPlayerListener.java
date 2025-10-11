package io.liftreloaded.paper.listener;

import io.liftreloaded.bukkit.listener.PlayerListener;
import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

public class PaperPlayerListener extends PlayerListener {

	private final Set<UUID> joinedPlayers = new HashSet<>();

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		joinedPlayers.add(event.getPlayer()
				.getUniqueId());
	}

	@EventHandler(ignoreCancelled = true)
	public void onLoadedWorld(PlayerClientLoadedWorldEvent event) {
		UUID playerUuid = event.getPlayer()
				.getUniqueId();
		if (joinedPlayers.remove(playerUuid)) {
			handleLogin(event);
		}
	}
}
