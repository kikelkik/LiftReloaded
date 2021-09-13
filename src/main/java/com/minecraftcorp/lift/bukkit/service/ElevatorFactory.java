package com.minecraftcorp.lift.bukkit.service;

import com.minecraftcorp.lift.bukkit.LiftPlugin;
import com.minecraftcorp.lift.bukkit.model.BukkitConfig;
import com.minecraftcorp.lift.bukkit.model.BukkitElevator;
import com.minecraftcorp.lift.bukkit.model.BukkitFloorSign;
import com.minecraftcorp.lift.common.exception.ElevatorCreateException;
import com.minecraftcorp.lift.common.exception.ElevatorUsageException;
import com.minecraftcorp.lift.common.model.Elevator;
import com.minecraftcorp.lift.common.model.Floor;
import com.minecraftcorp.lift.common.model.FloorSign;
import com.minecraftcorp.lift.common.model.Messages;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;

@UtilityClass
public class ElevatorFactory {

	public static final BukkitConfig config = BukkitConfig.INSTANCE;
	public static final Messages messages = Messages.INSTANCE;
	public static final LiftPlugin plugin = LiftPlugin.INSTANCE;

	public static Optional<BukkitElevator> createElevator(Block buttonBlock) {
		if (!config.isValidLiftStructureFromButton(buttonBlock)) {
			return Optional.empty();
		}

		Set<Block> baseBlocks = findBaseBlocksBelow(buttonBlock);
		if (baseBlocks.isEmpty()) {
			plugin.logDebug("Found no base block. Assuming this is not supposed to be an elevator.");
			return Optional.empty();
		}
		List<Floor> floors = createFloors(baseBlocks);
		if (floors.size() <= 1) {
			throw new ElevatorUsageException(messages.getOneFloor());
		}
		plugin.logDebug("Found " + baseBlocks.size() + " base blocks and " + floors.size() + " floors");
		Floor startFloor = getStartFloor(buttonBlock, floors);
		BukkitElevator elevator = new BukkitElevator(baseBlocks, startFloor, floors, findInitialSign(buttonBlock, startFloor));
		floors.stream()
				.flatMap(floor -> floor.getSigns().stream())
				.forEach(sign -> sign.setElevator(elevator));
		writeInvalidFloorSigns(elevator);
		return Optional.of(elevator);
	}

	private static BukkitFloorSign findInitialSign(Block buttonBlock, Floor startFloor) {
		BiFunction<BukkitFloorSign, Function<Location, Integer>, Integer> getCoord = (sign, locFunc) -> locFunc.apply(
				sign.getSign().getLocation());
		return startFloor.getSigns()
				.stream()
				.map(BukkitFloorSign.class::cast)
				.filter(sign -> getCoord.apply(sign, Location::getBlockX) == buttonBlock.getX())
				.filter(sign -> getCoord.apply(sign, Location::getBlockZ) == buttonBlock.getZ())
				.findFirst()
				.orElseThrow(() -> new ElevatorCreateException("Could not extract initial floor sign from start floor"));
	}

	public static void writeInvalidFloorSigns(Elevator elevator) {
		List<Floor> floors = elevator.getFloors();
		for (int level = 0; level < floors.size(); level++) {
			Floor current = floors.get(level);
			boolean anyInvalid = !current.getSigns()
					.stream()
					.allMatch(FloorSign::isValid);
			if (anyInvalid) {
				current.updateSigns(floors.get(level == floors.size() - 1 ? 0 : level + 1));
			}
		}
	}

	private static Floor getStartFloor(Block buttonBlock, List<Floor> floors) {
		Optional<Floor> startFloor = floors.stream()
				.filter(floor -> floor.getButtonY() == buttonBlock.getY())
				.findFirst();
		if (startFloor.isEmpty()) {
			throw new ElevatorCreateException("Could not extract start floor from elevator's floors");
		}
		return startFloor.get();
	}

	private static List<Floor> createFloors(Set<Block> baseBlocks) {
		List<Floor> floors = new ArrayList<>();
		Optional<Block> firstBase = baseBlocks.stream().findFirst();
		if (firstBase.isEmpty()) {
			return Collections.emptyList();
		}
		int level = 1;
		World world = firstBase.get().getWorld();
		for (int y = firstBase.get().getY() + 1; y < config.getMaxHeight(); y++) {
			List<Block> buttons = new ArrayList<>();
			for (Block baseBlock : baseBlocks) {
				int x = baseBlock.getX();
				int z = baseBlock.getZ();

				Block block = world.getBlockAt(x, y, z);
				if (!isValidShaftBlock(block)) {
					return floors;
				}
				if (config.isValidLiftStructureFromButton(block)) {
					buttons.add(block);
				}
			}
			if (!buttons.isEmpty()) {
				floors.add(createFloor(buttons, level));
				level++;
			}
		}
		return floors;
	}

	private static Floor createFloor(List<Block> buttonBlocks, int level) {
		if (buttonBlocks.isEmpty()) {
			throw new ElevatorCreateException("Cannot create floor with no button blocks");
		}
		List<FloorSign> floorSigns = buttonBlocks.stream()
				.map(button -> button.getRelative(BlockFace.UP))
				.map(Block::getState)
				.map(Sign.class::cast)
				.map(BukkitFloorSign::new)
				.collect(Collectors.toList());
		String floorName = buttonBlocks.stream()
				.map(block -> block.getRelative(BlockFace.DOWN))
				.filter(config::isSign)
				.map(Block::getState)
				.map(Sign.class::cast)
				.map(sign -> sign.getLine(1))
				.findFirst()
				.orElse("");
		return new Floor(level, floorName, buttonBlocks.get(0).getY(), floorSigns);
	}

	private static Set<Block> findBaseBlocksBelow(Block startBlock) {
		World world = startBlock.getWorld();
		int x = startBlock.getX();
		int z = startBlock.getZ();
		for (int y = startBlock.getY(); y >= 0; y--) {
			Block block = world.getBlockAt(x, y, z);
			if (isValidShaftBlock(block)) {
				continue;
			}
			if (!config.isBaseBlock(block)) {
				return Collections.emptySet();
			}
			return getNeighborBaseBlocks(block, new HashSet<>());
		}
		return Collections.emptySet();
	}

	private static Set<Block> getNeighborBaseBlocks(Block baseBlock, Set<Block> blocks) {
		if (blocks.size() == config.getMaxLiftArea()) {
			plugin.logDebug("Reached limit of " + blocks.size() + " base blocks (see max lift area in config)");
			return blocks;
		}
		blocks.add(baseBlock);
		Material baseType = baseBlock.getType();
		List<BlockFace> blockFaces = Arrays.asList(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);
		for (BlockFace blockFace : blockFaces) {
			Block neighbor = baseBlock.getRelative(blockFace);
			if (!blocks.contains(neighbor) && neighbor.getType() == baseType) {
				blocks.addAll(getNeighborBaseBlocks(neighbor, blocks));
			}
		}
		return blocks;
	}

	private static boolean isValidShaftBlock(Block block){
		return !block.getType().isSolid()
				|| config.getFloorMaterials().contains(block.getType())
				|| config.isButton(block)
				|| config.isSign(block)
				|| config.isShaftBlock(block);
	}
}
