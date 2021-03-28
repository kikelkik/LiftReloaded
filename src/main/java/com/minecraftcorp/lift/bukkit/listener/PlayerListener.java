package com.minecraftcorp.lift.bukkit.listener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import com.minecraftcorp.lift.bukkit.LiftPlugin;
import com.minecraftcorp.lift.bukkit.model.BukkitConfig;
import com.minecraftcorp.lift.bukkit.model.BukkitElevator;
import com.minecraftcorp.lift.bukkit.model.BukkitFloorSign;
import com.minecraftcorp.lift.bukkit.service.ElevatorExecutor;
import com.minecraftcorp.lift.bukkit.service.ElevatorFactory;
import com.minecraftcorp.lift.common.exception.ElevatorException;
import com.minecraftcorp.lift.common.exception.ElevatorUsageException;
import com.minecraftcorp.lift.common.model.Elevator;
import com.minecraftcorp.lift.common.model.Floor;
import com.minecraftcorp.lift.common.model.FloorSign;
import com.minecraftcorp.lift.common.model.Messages;
import com.minecraftcorp.lift.common.model.Permission;
import com.minecraftcorp.lift.common.util.Calculator;

public class PlayerListener implements Listener {

	private final LiftPlugin plugin = LiftPlugin.INSTANCE;
	private final BukkitConfig config = BukkitConfig.INSTANCE;
	private final Messages messages = Messages.INSTANCE;
	private final Map<UUID, FloorSign> activeScrollSelects = new HashMap<>();
	private final Map<UUID, Location> quitInElevator = new HashMap<>();

	public PlayerListener() {
		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (Action.RIGHT_CLICK_BLOCK != event.getAction()) {
			return;
		}
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		if (block == null) {
			return;
		}
		// Switch Floors
		if (config.isValidLiftStructureFromButton(block.getRelative(BlockFace.DOWN))) {
			if (!Permission.hasPermission(player, Permission.CHANGE)) {
				Permission.sendMessage(player, Permission.CHANGE);
				return;
			}
			player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, config.relativeVolume(1), 1);
			UUID uuid = player.getUniqueId();
			if (activeScrollSelects.containsKey(uuid)) {
				activeScrollSelects.remove(uuid);
				event.setCancelled(true);
				player.sendMessage(messages.getScrollSelectDisabled());
				return;
			}
			if (((Sign) block.getState()).getLine(0).isEmpty()) {
				plugin.logDebug("Performing elevator floor scan");
				createElevator(block.getRelative(BlockFace.DOWN), player);
				return;
			}
			selectNextFloor(block, player);
			return;
		}
		// Start Elevator
		if (config.isValidLiftStructureFromButton(block)) {
			 createAndRunElevator(block, player);
		}
	}

	@EventHandler
	public void onHeldItemChange(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		if (!activeScrollSelects.containsKey(uuid)) {
			return;
		}
		boolean scrollForwards = Calculator.isScrollForwards(event.getNewSlot(), event.getPreviousSlot());
		FloorSign floorSign = activeScrollSelects.get(uuid);
		boolean tooFarAway = Optional.of(floorSign)
				.map(BukkitFloorSign.class::cast)
				.map(BukkitFloorSign::getSign)
				.map(BlockState::getLocation)
				.map(location -> location.distance(player.getLocation()))
				.filter(distance -> distance > 3)
				.isPresent();
		if (tooFarAway) {
			activeScrollSelects.remove(player.getUniqueId());
			player.sendMessage(messages.getScrollSelectDisabled());
			return;
		}
		player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, config.relativeVolume(1), 2);
		setDestToNext(floorSign, player, scrollForwards);
	}

	/**
	 * If a player quits within an elevator, we have to save a location to teleport the player on next login, so he
	 * won't fall down the shaft.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onLogout(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (plugin.isInNoLift(player.getUniqueId())) {
			return;
		}
		Optional<BukkitElevator> elevatorOpt = plugin.getActiveLifts()
				.stream()
				.filter(elevator -> elevator.getPassengers().contains(player) || elevator.getFreezers().contains(player))
				.findFirst();
		if (!elevatorOpt.isPresent()) {
			plugin.logWarn(player.getName() + " is in any lift but the elevator could not be found");
			return;
		}
		BukkitElevator elevator = elevatorOpt.get();

		Location baseFloor = elevator.getCenter(elevator.getFloorByLevel(1));
		quitInElevator.put(player.getUniqueId(), baseFloor);
		getVehicleOfPlayer(player).ifPresent(vehicle -> quitInElevator.put(vehicle.getUniqueId(), baseFloor));
		plugin.logDebug("Remember that " + player.getName() + " quit within an elevator.");

		elevator.removePassengers(Collections.singletonList(player));
		elevator.removeFreezers(Collections.singletonList(player));
	}

	@EventHandler(ignoreCancelled = true)
	public void onLogin(PlayerSpawnLocationEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		if (!quitInElevator.containsKey(uuid)) {
			return;
		}
		Location baseFloor = quitInElevator.get(uuid);
		getVehicleOfPlayer(player).ifPresent(ElevatorExecutor::resetEntityPhysics);
		ElevatorExecutor.resetEntityPhysics(player);
		player.teleport(baseFloor, PlayerTeleportEvent.TeleportCause.PLUGIN); // FIXME: sometimes fall damage
		plugin.logDebug(player.getName() + " logged out in an elevator. Teleported to base floor at " + baseFloor);
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		if (!config.getPreventEntry()) {
			return;
		}
		Player player = event.getPlayer();
		boolean playerInShaft = plugin.getActiveLifts()
				.stream()
				.anyMatch(elevator -> !elevator.isOutsideShaft(player));
		if (!playerInShaft) {
			return;
		}
		boolean playerRidesLift = plugin.getActiveLifts()
				.stream()
				.flatMap(elevator -> Stream.concat(elevator.getPassengers()
						.stream(), elevator.getFreezers()
						.stream()))
				.filter(Player.class::isInstance)
				.anyMatch(entity -> entity.equals(player));
		if (playerRidesLift) {
			return;
		}
		player.sendMessage(messages.getCantEnter());
		Location to = event.getTo();
		if (to == null) {
			plugin.logWarn("Could not prevent lift entry for " + player.getName() + " at " + player.getLocation());
			return;
		}
		Vector pushBack = player.getVelocity().subtract(to.toVector())
				.normalize()
				.multiply(.3);
		player.setVelocity(pushBack);
	}

	private void selectNextFloor(Block signBlock, Player player) {
		Block button = signBlock.getRelative(BlockFace.DOWN);
		Optional<BukkitElevator> elevatorOpt = createElevator(button, player);
		if (!elevatorOpt.isPresent()) {
			return;
		}
		BukkitElevator elevator = elevatorOpt.get();
		Optional<Floor> currentFloorOpt = elevator.getFloorFromY(button.getY());
		if (!currentFloorOpt.isPresent()) {
			player.sendMessage(messages.getFloorNotExists());
			return;
		}
		// select by scroll
		if (config.getMouseScroll() && isEmptyHand(player)) {
			elevator.getFloorFromY(button.getY())
					.ifPresent(floor -> {
						activeScrollSelects.put(player.getUniqueId(), elevator.getInitialSign());
						player.sendMessage(messages.getScrollSelectEnabled());
					});
			return;
		}
		// select by click
		setDestToNext(elevator.getInitialSign(), player, true);
	}

	private void createAndRunElevator(Block buttonBlock, Player player) {
		Optional<BukkitElevator> elevator = createElevator(buttonBlock, player);
		if (!elevator.isPresent()) {
			return;
		}
		try {
			ElevatorExecutor.runElevator(elevator.get());
		} catch (ElevatorUsageException e) {
			catchElevatorUsageException(player, e);
		} catch (ElevatorException e) {
			handleElevatorException(player, e);
		}
	}

	private Optional<BukkitElevator> createElevator(Block buttonBlock, Player player) {
		try {
			return ElevatorFactory.createElevator(buttonBlock);
		} catch (ElevatorUsageException e) {
			catchElevatorUsageException(player, e);
		} catch (ElevatorException e) {
			handleElevatorException(player, e);
		}
		return Optional.empty();
	}

	private void setDestToNext(FloorSign floorSign, Player player, boolean isNextAbove) {
		Elevator elevator = floorSign.getElevator();
		Floor currentFloor = elevator.getFloorBySign(floorSign);
		int destLevel = floorSign.readDestLevel();
		try {
			Floor destFloor = elevator.getFloorByLevel(destLevel);
			Optional<Floor> nextFloor = isNextAbove ? elevator.getNextFloor(destFloor, currentFloor) : elevator.getPreviousFloor(destFloor, currentFloor);
			if (!nextFloor.isPresent()) {
				player.sendMessage(messages.getOneFloor());
				return;
			}
			elevator.setDestFloor(nextFloor.get());
			currentFloor.updateSigns(nextFloor.get());
		} catch (ElevatorUsageException e) {
			catchElevatorUsageException(player, e);
		} catch (ElevatorException e) {
			handleElevatorException(player, e);
		}
	}

	private void handleElevatorException(Player player, ElevatorException e) {
		player.sendMessage("§cAn internal error occurred while trying to process a lift");
		plugin.logError("An error occurred while trying to process a lift", e);
	}

	private void catchElevatorUsageException(Player player, ElevatorUsageException e) {
		player.sendMessage(e.getMessage());
		plugin.logDebug(player.getName() + " tried to use an elevator (" + e.getMessage() + ")");
	}

	private boolean isEmptyHand(Player player) {
		return player.getInventory()
				.getItemInMainHand()
				.getType() == Material.AIR;
	}

	private Optional<Entity> getVehicleOfPlayer(Player player) {
		return Optional.ofNullable(player.getVehicle());
	}
}
