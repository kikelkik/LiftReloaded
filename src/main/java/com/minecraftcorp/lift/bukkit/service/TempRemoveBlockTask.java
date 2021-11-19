package com.minecraftcorp.lift.bukkit.service;

import com.minecraftcorp.lift.bukkit.LiftPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;

public class TempRemoveBlockTask extends BukkitRunnable {

	private final Block block;
	private final BlockData blockData;

	public TempRemoveBlockTask(LiftPlugin plugin, Block block, int restoreDelay) {
		this.block = block;
		this.blockData = block.getBlockData();
		block.setType(Material.AIR);
		runTaskLater(plugin, restoreDelay);
	}

	@Override
	public void run() {
		block.setBlockData(blockData);
	}
}
