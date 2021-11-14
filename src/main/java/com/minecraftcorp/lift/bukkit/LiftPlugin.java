package com.minecraftcorp.lift.bukkit;

import com.minecraftcorp.lift.bukkit.command.LiftCommand;
import com.minecraftcorp.lift.bukkit.listener.PlayerListener;
import com.minecraftcorp.lift.bukkit.listener.RedstoneListener;
import com.minecraftcorp.lift.bukkit.listener.VehicleListener;
import com.minecraftcorp.lift.bukkit.model.BukkitConfig;
import com.minecraftcorp.lift.bukkit.model.BukkitElevator;
import com.minecraftcorp.lift.bukkit.service.sound.SoundTask;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Stream;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class LiftPlugin extends JavaPlugin {

	public static LiftPlugin INSTANCE;
	private static final BukkitConfig config = BukkitConfig.INSTANCE;
	private final Set<BukkitElevator> activeLifts = new HashSet<>();
	private boolean noteBlockAPIEnabled;

	@Override
	public void onEnable() {
		INSTANCE = this;
		new LiftCommand();
		new PlayerListener();
		new VehicleListener();

		noteBlockAPIEnabled = Bukkit.getPluginManager().isPluginEnabled("NoteBlockAPI");
		if (!noteBlockAPIEnabled) {
			logInfo("*** NoteBlockAPI is not installed or not enabled. ***");
		}

		reload();
		if (config.getRedstone()) {
			new RedstoneListener();
		}
	}

	@Override
	public void onDisable() {

	}

	public void reload() {
		config.loadConfig(this);
		SoundTask.reload();
		logInfo("Lift successfully reloaded");
	}

	public void logInfo(String message) {
		getLogger().info(message);
	}

	public void logWarn(String message) {
		getLogger().warning(message);
	}

	public void logError(String message, Throwable e) {
		getLogger().log(Level.SEVERE, message, e);
	}

	public void logDebug(String message) {
		if (config.getDebug()) {
			getLogger().info("[DEBUG] " + message);
		}
	}

	public void addActiveLift(BukkitElevator elevator) {
		activeLifts.add(elevator);
	}

	public void removeActiveLift(BukkitElevator elevator) {
		activeLifts.remove(elevator);
	}

	public boolean isInNoLift(UUID entityUuid) {
		return activeLifts.stream()
				.flatMap(lift -> Stream.concat(lift.getPassengers().stream(), lift.getFreezers().stream()))
				.noneMatch(entity -> entityUuid.equals(entity.getUniqueId()));
	}
}
