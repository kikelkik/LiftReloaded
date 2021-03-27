package com.minecraftcorp.lift.bukkit.service.sound;

import com.minecraftcorp.lift.bukkit.model.BukkitElevator;

public class SimpleSoundTask extends SoundTask {

	public SimpleSoundTask(BukkitElevator elevator) {
		super(elevator);
	}

	@Override
	public void run() {

	}

	@Override
	public synchronized void cancel() throws IllegalStateException {
		super.cancel();
	}
}
