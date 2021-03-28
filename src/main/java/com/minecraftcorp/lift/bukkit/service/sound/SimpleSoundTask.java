package com.minecraftcorp.lift.bukkit.service.sound;

import org.bukkit.Sound;

import com.minecraftcorp.lift.bukkit.model.BukkitElevator;

public class SimpleSoundTask extends SoundTask {

	SimpleSoundTask(BukkitElevator elevator) {
		super(elevator, 5);
	}

	@Override
	public void run() {
		filterPlayers(elevator.getPassengers()).forEach(
				player -> {
					player.playSound(player.getLocation(), Sound.ENTITY_BOAT_PADDLE_LAND, .8F, .5F);
					player.playSound(player.getLocation(), Sound.BLOCK_BEEHIVE_WORK, .5F, .5F);
				});
	}

	@Override
	public synchronized void cancel() throws IllegalStateException {
		super.cancel();
	}
}
