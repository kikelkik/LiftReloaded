package com.minecraftcorp.lift.bukkit.service.sound;

import java.util.Collection;
import java.util.stream.Stream;

import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.minecraftcorp.lift.bukkit.LiftPlugin;
import com.minecraftcorp.lift.bukkit.model.BukkitConfig;
import com.minecraftcorp.lift.bukkit.model.BukkitElevator;

public abstract class SoundTask extends BukkitRunnable {

	protected static final BukkitConfig config = BukkitConfig.INSTANCE;
	protected static final LiftPlugin plugin = LiftPlugin.INSTANCE;
	protected static final int volume = config.getSoundVolume();
	protected final BukkitElevator elevator;

	public static SoundTask create(BukkitElevator elevator) {
		return config.isMusicEnabled(elevator.getBase()) ? new RadioSoundTask(elevator) : new SimpleSoundTask(elevator);
	}

	protected SoundTask(BukkitElevator elevator, int taskInterval) {
		this.elevator = elevator;
		runTaskTimer(plugin, 0, taskInterval);
		plugin.logDebug("Started " + getClass().getSimpleName());
	}

	@Override
	public synchronized void cancel() throws IllegalStateException {
		super.cancel();
		filterPlayers(elevator.getFreezers()).forEach(player -> player.playSound(player.getLocation(),
				Sound.BLOCK_NOTE_BLOCK_BELL, config.relativeVolume(1), .5F));
	}

	protected Stream<Player> filterPlayers(Collection<Entity> entities) {
		return entities.stream()
				.filter(Player.class::isInstance)
				.map(Player.class::cast);
	}

	public static void reload() {
		RadioSoundTask.reload();
	}
}
