package com.minecraftcorp.lift.bukkit.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.minecraftcorp.lift.bukkit.LiftPlugin;
import com.minecraftcorp.lift.common.exception.ConfigurationException;
import com.minecraftcorp.lift.common.exception.ElevatorException;
import com.minecraftcorp.lift.common.model.Config;
import com.minecraftcorp.lift.common.model.Messages;
import com.minecraftcorp.lift.common.util.ConfigReader;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Note that fields that are configurable in config.yml should have the same name, so
 * {@link ConfigReader#mapConfigurationToFields )} maps values correctly to fields of this class.
 * For that mapping, you should use boxed types instead of primitive types (Integer instead of int, ...)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BukkitConfig extends Config {

	public static final BukkitConfig INSTANCE = new BukkitConfig();
	private static final LiftPlugin plugin = LiftPlugin.INSTANCE;

	private final Map<Material, Double> blockSpeeds = new HashMap<>();
	private final Set<Material> floorMaterials = new HashSet<>();
	private final Set<Material> buttonMaterials = new HashSet<>();
	private final Set<Material> signMaterials = new HashSet<>();
	private final Set<Material> musicBlocks = new HashSet<>();
	private final Set<Material> shaftBlocks = new HashSet<>();

	private boolean useNoCheatPlus;
	private boolean serverFlightAllowed;

	public boolean isSign(Block block) {
		return signMaterials.contains(block.getType()) && block.getState() instanceof Sign;
	}

	public boolean isButton(Block block) {
		return buttonMaterials.contains(block.getType());
	}

	public boolean isBaseBlock(Block block) {
		return blockSpeeds.containsKey(block.getType());
	}

	public boolean isFloorBlock(Block block) {
		return floorMaterials.contains(block.getType());
	}

	public double getBlockSpeed(Block block) {
		if (blockSpeeds.containsKey(block.getType())) {
			return blockSpeeds.get(block.getType());
		}
		throw new ElevatorException("Block speed not configured for " + block.getType());
	}

	public boolean isMusicEnabled(Block block) {
		return plugin.isNoteBlockAPIEnabled() && musicBlocks.contains(block.getType());
	}

	public boolean isShaftBlock(Block block) {
		return shaftBlocks.contains(block.getType());
	}

	public boolean isValidLiftStructureFromButton(Block buttonBlock) {
		Block floorBlock = buttonBlock.getRelative(BlockFace.DOWN, 2);
		return isButton(buttonBlock) && isSign(buttonBlock.getRelative(BlockFace.UP)) &&
				(isFloorBlock(floorBlock) || isBaseBlock(floorBlock));
	}

	public void loadConfig(LiftPlugin plugin) {
		File configFile = new File(plugin.getDataFolder(), File.separator + "config.yml");
		File defaultConfigFile = new File(plugin.getDataFolder(),
				File.separator + "default" + File.separator + "config.yml");
		if (!configFile.exists()) {
			copyDefaultConfig(plugin, configFile);
		}

		YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "/config" +
				".yml"));
		config.setDefaults(getDefaultConfig(plugin, defaultConfigFile));
		config.options()
				.copyDefaults(true);

		// defaultConfig was just temporarily for setting default config values
		deleteDefaultConfig(plugin, defaultConfigFile);

		mapConfiguration(config, this, Config.class);
		mapConfiguration(config.getConfigurationSection("messages"), Messages.INSTANCE, Messages.class);

		ConfigurationSection baseBlocks = config.getConfigurationSection("baseBlocks");
		for (String block : Objects.requireNonNull(baseBlocks).getKeys(false)) {
			Material material = Material.valueOf(block);
			blockSpeeds.put(material, baseBlocks.getDouble(block + ".speed"));
			if (baseBlocks.contains(block + ".music") && baseBlocks.getBoolean(block + ".music")) {
				musicBlocks.add(material);
			}
		}

		fillMaterialFromConfig(config, "floorBlocks", floorMaterials);
		plugin.logDebug("Floor materials added: " + floorMaterials);

		fillMaterialFromConfig(config, "buttonBlocks", buttonMaterials);
		plugin.logDebug("Button materials added: " + buttonMaterials);

		fillMaterialFromConfig(config, "signBlocks", signMaterials);
		plugin.logDebug("Sign materials added: " + signMaterials);

		fillMaterialFromConfig(config, "shaftBlocks", shaftBlocks);
		plugin.logDebug("Allowed shaft blocks added: " + shaftBlocks);

		try {
			validate();
			config.save(configFile);
		} catch (IOException | ConfigurationException e) {
			throw new ConfigurationException("Could not save config to " + configFile, e);
		}

		serverFlightAllowed = plugin.getServer()
				.getAllowFlight();

		if (plugin.getServer()
				.getPluginManager()
				.getPlugin("NoCheatPlus") != null) {
			useNoCheatPlus = true;
			plugin.logDebug("Hooked into NoCheatPlus");
		}
	}

	private void mapConfiguration(ConfigurationSection section, Object object, Class<?> clazz) {
		if (section == null) return;

		BiFunction<String, Class<?>, Object> valueResolver = (name, fieldType) ->
				fieldType != String.class ? section.getObject(name, fieldType) : section.getString(name)
						.replace("&", "§");

		ConfigReader.mapConfigurationToFields(section.getKeys(false), object, valueResolver, clazz);
	}

	protected void validate() {
		super.validate();
		List<String> emptySets = new ArrayList<>();
		if (blockSpeeds.isEmpty()) {
			emptySets.add("blockSpeeds");
		}
		if (floorMaterials.isEmpty()) {
			emptySets.add("floorMaterials");
		}
		if (signMaterials.isEmpty()) {
			emptySets.add("signMaterials");
		}
		if (buttonMaterials.isEmpty()) {
			emptySets.add("buttonMaterials");
		}
		if (!emptySets.isEmpty()) {
			plugin.logWarn(String.join(", ", emptySets) + " is empty in config.yml. " +
					"No Lift will work");
		}
		if (!musicBlocks.isEmpty() && !plugin.isNoteBlockAPIEnabled()) {
			plugin.logWarn("You have configured base blocks with music but NoteBlockAPI is not installed. " +
					"Music won't work");
		}
	}

	private void fillMaterialFromConfig(YamlConfiguration config, String configKey, Set<Material> materialSet) {
		List<String> configShaftMaterials = config.getStringList(configKey);
		Arrays.stream(Material.values())
				.filter(mat -> anyMaterialMatch().test(configShaftMaterials, mat))
				.forEach(materialSet::add);
	}

	private static BiPredicate<List<String>, Material> anyMaterialMatch() {
		return (list, mat) -> list.stream()
				.anyMatch(configMat -> mat.name()
						.matches(configMat.toUpperCase()
								.replace("*", ".*?")));
	}



	private static YamlConfiguration getDefaultConfig(LiftPlugin plugin, File defaultConfigFile) {
		copyDefaultConfig(plugin, defaultConfigFile);
		return YamlConfiguration.loadConfiguration(defaultConfigFile);
	}
}
