package io.liftreloaded.bukkit;

import io.liftreloaded.bukkit.command.LiftCommand;
import io.liftreloaded.bukkit.listener.PlayerListener;
import io.liftreloaded.bukkit.listener.VehicleListener;
import io.liftreloaded.bukkit.model.BukkitConfig;
import io.liftreloaded.bukkit.model.BukkitElevator;
import io.liftreloaded.bukkit.service.sound.SoundTask;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class LiftPlugin {

	public static LiftPlugin INSTANCE = new LiftPlugin();
	@Delegate
	private JavaPlugin delegate;
	private static final BukkitConfig config = BukkitConfig.INSTANCE;
	private final Set<BukkitElevator> activeLifts = new HashSet<>();
	private boolean noteBlockAPIEnabled;

	public void enablePlugin(JavaPlugin delegate, Supplier<PlayerListener> playerListenerSupplier) {
		this.delegate = delegate;

		Objects.requireNonNull(delegate.getServer()
						.getPluginCommand("lift"))
				.setExecutor(new LiftCommand());
		Bukkit.getServer()
				.getPluginManager()
				.registerEvents(new VehicleListener(), delegate);
		Bukkit.getServer()
				.getPluginManager()
				.registerEvents(playerListenerSupplier.get(), delegate);

		noteBlockAPIEnabled = Bukkit.getPluginManager().isPluginEnabled("NoteBlockAPI");
		if (!noteBlockAPIEnabled) {
			logInfo("*** NoteBlockAPI is not installed or not enabled. ***");
		}

		reload();
	}

	public void reload() {
		config.loadConfig(this);
		SoundTask.reload();
		logInfo("Lift successfully reloaded");
	}

	public void logInfo(String message) {
		delegate.getLogger().info(message);
	}

	public void logWarn(String message) {
		delegate.getLogger().warning(message);
	}

	public void logError(String message, Throwable e) {
		delegate.getLogger().log(Level.SEVERE, message, e);
	}

	public void logDebug(String message) {
		if (config.getDebug()) {
			delegate.getLogger().info("[DEBUG] " + message);
		}
	}

	public void addActiveLift(BukkitElevator elevator) {
		activeLifts.add(elevator);
	}

	public void removeActiveLift(BukkitElevator elevator) {
		activeLifts.remove(elevator);
	}

	public boolean isInNoLift(Entity entity) {
		return getUsingElevators(entity).isEmpty();
	}

	/**
	 * Get all elevators that have 'usingEntity' as passenger or freezer
	 */
	public List<BukkitElevator> getUsingElevators(Entity usingEntity) {
		return activeLifts.stream()
				.filter(elevator -> elevator.getInvolvedEntities()
						.map(Entity::getUniqueId)
						.anyMatch(e -> e == usingEntity.getUniqueId()))
				.toList();
	}
}
