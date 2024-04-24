package com.minecraftcorp.lift.bukkit.service;

import com.minecraftcorp.lift.bukkit.LiftPlugin;
import com.minecraftcorp.lift.bukkit.model.BukkitConfig;
import com.minecraftcorp.lift.bukkit.model.BukkitElevator;
import com.minecraftcorp.lift.common.model.Floor;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

@UtilityClass
public class ElevatorExecutor {

	private static final BukkitConfig config = BukkitConfig.INSTANCE;
	private static final LiftPlugin plugin = LiftPlugin.INSTANCE;
	private static final List<Material> RAIL_MATERIALS = Arrays.asList(Material.RAIL, Material.DETECTOR_RAIL,
			Material.ACTIVATOR_RAIL, Material.POWERED_RAIL);

	public static void runElevator(BukkitElevator elevator) {
		elevator.setDestFloorFromSign();

		List<Entity> entities = findEntities(elevator);
		List<Entity> passengers = extractPassengers(entities, elevator.getStartFloor());
		Set<Entity> freezers = extractFreezers(entities, passengers);
		if (passengers.isEmpty()) {
			plugin.logDebug("No passengers in Elevator");
			return;
		}

		elevator.addPassengers(passengers);
		elevator.addFreezers(freezers);
		if (config.getAutoPlace()) {
			tpPassengersToFloor(elevator, elevator.getStartFloor());
		}
		removeFloorBlocks(elevator);
		tempRemoveRailsUnderMinecarts(passengers);
		elevator.initTimeMeasures();

		new ElevatorTask(elevator);
	}

	private static void tempRemoveRailsUnderMinecarts(List<Entity> passengers) {
		passengers.stream()
				.filter(Minecart.class::isInstance)
				.map(Entity::getLocation)
				.map(Location::getBlock)
				.filter(block -> RAIL_MATERIALS.contains(block.getType()))
				.forEach(rail -> new TempRemoveBlockTask(plugin, rail, 10));
	}

	public static void tpPassengersToFloor(BukkitElevator elevator, Floor floor) {
		Location destination = elevator.getCenter(floor);
		for (Entity entity : elevator.getPassengers()) {
			Location location = entity.getLocation();
			location.setX(destination.getX());
			location.setY(destination.getY());
			location.setZ(destination.getZ());
			entity.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
		}
	}

	public static void resetEntityPhysics(Entity entity) {
		entity.setFallDistance(0);
		entity.setVelocity(new Vector(0, 0, 0));
		entity.setGravity(true);
		if (entity instanceof Player) {
			NoCheatModifier.resetFlightRules(((Player) entity));
		}
	}

	public static void prepareEntityPhysics(Entity entity) {
		entity.setGravity(false);
		if (entity instanceof Player) {
			NoCheatModifier.setFlightRules((Player) entity);
		}
	}

	private static void removeFloorBlocks(BukkitElevator elevator) {
		List<Floor> floorsToRemove = elevator.getFloorsToRemove();
		World world = elevator.getWorld();
		for (Block baseBlock : elevator.getBaseBlocks()) {
			for (Floor floor : floorsToRemove) {
				Block block = world.getBlockAt(baseBlock.getX(), floor.getFloorY(), baseBlock.getZ());
				if (!config.isFloorBlock(block)) {
					continue;
				}
				Block blockAbove = block.getRelative(BlockFace.UP);
				if (!blockAbove.isEmpty() && !config.isSign(blockAbove)) {
					// shaft block like carpet, rail, redstone, ...
					removeAndSaveBlock(elevator, blockAbove);
				}
				removeAndSaveBlock(elevator, block);
			}
		}
	}

	private static void removeAndSaveBlock(BukkitElevator elevator, Block block) {
		elevator.saveBlock(block.getState());
		block.setType(Material.AIR);
	}

	private static List<Entity> findEntities(BukkitElevator elevator) {
		BoundingBox shaftArea = elevator.getShaftArea();
		List<Entity> entities = elevator.getWorld()
				.getNearbyEntities(shaftArea, entity -> config.getLiftMobs() || entity instanceof Player)
				.stream()
				.filter(plugin::isInNoLift)
				.toList();
		plugin.logDebug("Found " + entities.size() + " entities in " + shaftArea);
		return entities;
	}

	private static List<Entity> extractPassengers(List<Entity> entities, Floor floor) {
		return entities.stream()
				.filter(entity -> isEntityOnFloor(floor, entity))
				.toList();
	}

	private static Set<Entity> extractFreezers(List<Entity> entities, List<Entity> passengers) {
		return entities.stream()
				.filter(entity -> !passengers.contains(entity))
				.collect(Collectors.toSet());
	}

	private static boolean isEntityOnFloor(Floor floor, Entity entity) {
		double y = entity.getLocation().getY();
		if (entity.isInsideVehicle()) {
			y -= Objects.requireNonNull(entity.getVehicle()).getHeight();
		}
		return y >= floor.getFloorY() && y <= floor.getFloorY() + 2;
	}
}
