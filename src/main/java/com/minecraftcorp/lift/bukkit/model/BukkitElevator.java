package com.minecraftcorp.lift.bukkit.model;

import static com.minecraftcorp.lift.common.util.Calculator.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.minecraftcorp.lift.common.exception.ElevatorException;
import com.minecraftcorp.lift.common.exception.ElevatorRunException;
import com.minecraftcorp.lift.common.model.Elevator;
import com.minecraftcorp.lift.common.model.Floor;

import lombok.Getter;

@Getter
public class BukkitElevator extends Elevator {

	private static final double DEFAULT_SPEED = 0.5;
	private final BukkitConfig config = BukkitConfig.INSTANCE;
	private final Set<Block> baseBlocks;
	private final Set<Entity> passengers = new HashSet<>();
	private final Set<Entity> freezers = new HashSet<>();
	private final Set<BlockState> blockCache = new HashSet<>();
	private BoundingBox shaftArea;

	public BukkitElevator(Set<Block> baseBlocks, Floor startFloor, List<Floor> floors) {
		super(floors, startFloor, baseBlocks.stream()
				.map(BukkitConfig.INSTANCE::getBlockSpeed)
				.findAny()
				.orElse(DEFAULT_SPEED));
		this.baseBlocks = baseBlocks;
	}

	public void addPassengers(Collection<Entity> passengers) {
		this.passengers.addAll(passengers);
	}

	public void removePassengers(Collection<Entity> passengers) {
		this.passengers.removeAll(passengers);
	}

	public void clearPassengers() {
		passengers.clear();
	}

	public void addFreezers(Collection<Entity> freezers) {
		this.freezers.addAll(freezers);
	}

	public void removeFreezers(List<Entity> freezers) {
		this.freezers.removeAll(freezers);
	}

	public void saveBlock(BlockState blockState) {
		blockCache.add(blockState);
	}

	public World getWorld() {
		return getBase().getWorld();
	}

	public BoundingBox getShaftArea() {
		if (shaftArea != null) {
			return shaftArea;
		}
		int maxY = floors.stream()
				.max(Comparator.comparingInt(Floor::getButtonY))
				.map(Floor::getButtonY)
				.orElse(config.getMaxHeight()) + 1;

		Optional<Location> minBlock = baseBlocks.stream()
				.min((b1, b2) -> compareBlockCoords(b1.getX(), b1.getZ(), b2.getX(), b2.getZ()))
				.map(Block::getLocation);
		Optional<Location> maxBlock = baseBlocks.stream()
				.max((b1, b2) -> compareBlockCoords(b1.getX(), b1.getZ(), b2.getX(), b2.getZ()))
				.map(Block::getLocation);
		if (!minBlock.isPresent() || !maxBlock.isPresent()) {
			throw new ElevatorRunException("Could not find corner base blocks");
		}
		Location bottomCorner = minBlock.get();
		Location topCorner = maxBlock.get().clone();
		topCorner.setX(topCorner.getX() + 1);
		topCorner.setY(maxY);
		topCorner.setZ(topCorner.getZ() + 1);
		shaftArea = BoundingBox.of(bottomCorner, topCorner);
		return shaftArea;
	}

	public Location getCenter(Floor floor) {
		Vector shaftCenter = getShaftArea().getCenter();
		return new Location(getWorld(), shaftCenter.getX(), floor.getFloorY() + 1.5, shaftCenter.getZ());
	}

	public boolean isOutsideShaft(Entity entity) {
		return !getShaftArea().overlaps(entity.getBoundingBox());
	}

	public Block getBase() {
		Optional<Block> block = baseBlocks.stream()
				.findFirst();
		if (!block.isPresent()) {
			throw new ElevatorException("Could not get current world: base blocks are empty");
		}
		return block.get();
	}
}
