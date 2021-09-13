package com.minecraftcorp.lift.bukkit.listener;

import com.minecraftcorp.lift.bukkit.LiftPlugin;
import com.minecraftcorp.lift.bukkit.model.BukkitConfig;
import com.minecraftcorp.lift.bukkit.model.BukkitElevator;
import com.minecraftcorp.lift.bukkit.service.ElevatorExecutor;
import com.minecraftcorp.lift.bukkit.service.ElevatorFactory;
import com.minecraftcorp.lift.common.exception.ElevatorCreateException;
import com.minecraftcorp.lift.common.exception.ElevatorException;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

public class RedstoneListener implements Listener {

	private final LiftPlugin plugin = LiftPlugin.INSTANCE;
	private final BukkitConfig config = BukkitConfig.INSTANCE;

	public RedstoneListener() {
		Bukkit.getServer().getPluginManager().registerEvents(this, LiftPlugin.INSTANCE);
	}

	@EventHandler
	public void doRedstoneScan(BlockRedstoneEvent event) {
		if (!config.getRedstone()) {
			return;
		}
		Block block = event.getBlock();
		if (!block.isBlockPowered()) {
			return;
		}

		Optional<Block> buttonOpt = findButton(block);
		if (buttonOpt.isEmpty()) {
			return;
		}
		createAndRunElevator(buttonOpt.get());
	}

	private void createAndRunElevator(Block button) {
		try {
			Optional<BukkitElevator> elevator = ElevatorFactory.createElevator(button);
			if (elevator.isEmpty()) {
				return;
			}
			ElevatorExecutor.runElevator(elevator.get());
		} catch (ElevatorCreateException e) {
			plugin.logError("An error occurred while trying to create a lift", e);
		} catch (ElevatorException ignored) {
			// do nothing
		}
	}

	private Optional<Block> findButton(Block block) {
		Block[] adjacents = new Block[4];
		adjacents[0] = block.getRelative(BlockFace.EAST);
		adjacents[1] = block.getRelative(BlockFace.WEST);
		adjacents[2] = block.getRelative(BlockFace.NORTH);
		adjacents[3] = block.getRelative(BlockFace.SOUTH);

		for (Block adjacent : adjacents) {
			Block[] nextAdjacents = new Block[4];
			nextAdjacents[0] = adjacent.getRelative(BlockFace.EAST);
			nextAdjacents[1] = adjacent.getRelative(BlockFace.WEST);
			nextAdjacents[2] = adjacent.getRelative(BlockFace.NORTH);
			nextAdjacents[3] = adjacent.getRelative(BlockFace.SOUTH);
			for (Block nextAdjacent : nextAdjacents) {
				if (config.isValidLiftStructureFromButton(nextAdjacent)) {
					return Optional.of(nextAdjacent);
				}
			}
		}
		return Optional.empty();
	}
}
