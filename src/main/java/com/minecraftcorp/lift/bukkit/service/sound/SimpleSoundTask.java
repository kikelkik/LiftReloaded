package com.minecraftcorp.lift.bukkit.service.sound;

import org.bukkit.Sound;

import com.minecraftcorp.lift.bukkit.model.BukkitElevator;

public class SimpleSoundTask extends SoundTask {

	SimpleSoundTask(BukkitElevator elevator) {
		super(elevator, 5);
	}

	@Override
	public void run() {
		super.run();
		filterPlayers(elevator.getPassengers()).forEach(player -> {
			player.playSound(player.getLocation(), Sound.ENTITY_BOAT_PADDLE_LAND, config.relativeVolume(.8), .5F);
			player.playSound(player.getLocation(), Sound.BLOCK_BEEHIVE_WORK, config.relativeVolume(.5), .5F);
		});
	}
}
