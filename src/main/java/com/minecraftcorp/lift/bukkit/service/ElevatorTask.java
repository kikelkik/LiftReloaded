package com.minecraftcorp.lift.bukkit.service;

import com.minecraftcorp.lift.bukkit.LiftPlugin;
import com.minecraftcorp.lift.bukkit.model.BukkitConfig;
import com.minecraftcorp.lift.bukkit.model.BukkitElevator;
import com.minecraftcorp.lift.bukkit.service.sound.SoundTask;
import com.minecraftcorp.lift.common.model.Messages;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ElevatorTask extends BukkitRunnable {

	private static final BukkitConfig config = BukkitConfig.INSTANCE;
	private static final Messages messages = Messages.INSTANCE;
	private static final LiftPlugin plugin = LiftPlugin.INSTANCE;
	private final BukkitElevator elevator;
	private SoundTask soundTask;

	public ElevatorTask(BukkitElevator elevator) {
		this.elevator = elevator;
		Stream.concat(elevator.getPassengers().stream(), elevator.getFreezers().stream())
				.forEach(ElevatorExecutor::prepareEntityPhysics);

		plugin.addActiveLift(elevator);
		if (config.getSoundEnabled()) {
			soundTask = SoundTask.create(elevator);
		}
		runTaskTimer(plugin, 0, 2);
	}

	@Override
	public void run() {
		Set<Entity> passengers = elevator.getPassengers();
		if (passengers.isEmpty()) {
			restoreFloorBlocks();
			elevator.getFreezers()
					.forEach(ElevatorExecutor::resetEntityPhysics);

			plugin.removeActiveLift(elevator);
			plugin.logDebug("Elevator finished");

			if (soundTask != null) {
				soundTask.cancel();
			}
			cancel();
			return;
		}
		if (System.currentTimeMillis() > elevator.getMaxEndTime()) {
			timeout();
			elevator.clearPassengers();
			return;
		}
		List<Entity> passengersAtDest = passengers.stream()
				.filter(this::reachedDestination)
				.peek(entity -> plugin.logDebug(entity.getName() + " reached destination and waits for all passengers"))
				.collect(Collectors.toList());

		elevator.removePassengers(passengersAtDest);
		elevator.addFreezers(passengersAtDest);

		handleLeavingPassengers();

		elevator.getPassengers()
				.stream()
				.filter(entity -> !entity.isInsideVehicle())
				.forEach(this::refreshVelocity);
		elevator.getFreezers()
				.forEach(ElevatorTask::freezeEntity);
	}

	private void timeout() {
		plugin.logDebug("Elevator timeout after " + (elevator.getMaxEndTime() - elevator.getStartTime()) + " ms");
		ElevatorExecutor.tpPassengersToFloor(elevator, elevator.getDestFloor());
		Set<Entity> passengers = elevator.getPassengers();
		passengers.stream()
				.filter(Player.class::isInstance)
				.map(Player.class::cast)
				.forEach(player -> player.sendMessage(messages.getTimeout()));
		elevator.addFreezers(passengers);
	}

	private static void freezeEntity(Entity entity) {
		if (entity.isInsideVehicle()) {
			return;
		}
		entity.setVelocity(new Vector(0, 0, 0));
		entity.setFallDistance(0.0F);
	}

	private void refreshVelocity(Entity passenger) {
		passenger.setVelocity(new Vector(0, elevator.isGoingUp() ? elevator.getSpeed() : -elevator.getSpeed(), 0));
		passenger.setFallDistance(0);
	}

	private void handleLeavingPassengers() {
		Set<Entity> passengers = elevator.getPassengers();
		List<Entity> leavers = passengers.stream()
				.filter(elevator::isOutsideShaft)
				.collect(Collectors.toList());
		if (leavers.isEmpty()) {
			return;
		}
		if (config.getPreventLeave()) {
			Vector center = elevator.getShaftArea()
					.getCenter();

			for (Entity leaver : leavers) {
				Location location = leaver.getLocation();
				location.setX(center.getX());
				location.setZ(center.getZ());
				leaver.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
			}
		} else {
			leavers.forEach(ElevatorExecutor::resetEntityPhysics);
			elevator.removePassengers(leavers);
			plugin.logDebug("Leaving passengers: " + leavers);
		}
	}

	private boolean reachedDestination(Entity entity) {
		return elevator.isGoingUp() ?
				entity.getBoundingBox().getMinY() > elevator.getDestFloor().getFloorY() + 1:
				entity.getBoundingBox().getMinY() < elevator.getDestFloor().getButtonY();
	}

	private void restoreFloorBlocks() {
		elevator.getBlockCache()
				.stream()
				.sorted(Comparator.comparingInt(BlockState::getY))
				.forEach(blockState -> blockState.update(true));
	}
}
