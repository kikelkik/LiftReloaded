package com.minecraftcorp.lift.bukkit.listener;

import com.minecraftcorp.lift.bukkit.LiftPlugin;
import com.minecraftcorp.lift.common.model.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class VehicleListener implements Listener {

	private final LiftPlugin plugin = LiftPlugin.INSTANCE;
	private final Messages messages = Messages.INSTANCE;

	public VehicleListener() {
		Bukkit.getServer().getPluginManager().registerEvents(this, LiftPlugin.INSTANCE);
	}

	/**
	 * Prevent players from ejecting vehicles while within an elevator
	 */
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onEntityEject(VehicleExitEvent event){
		LivingEntity ejector = event.getExited();
		if (plugin.isInNoLift(event.getVehicle())) {
			return;
		}
		if (ejector instanceof Player) {
			ejector.sendMessage(messages.getUnsafe());
		}
		event.setCancelled(true);
		plugin.logDebug("Canceled ejection for " + ejector);
	}
}
