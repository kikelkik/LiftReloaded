package com.minecraftcorp.lift.bukkit.model;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Note that fields that are configurable in config.yml should have the same name, so
 * {@link #mapConfigurationToFields )} maps values correctly to fields of this class.
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
		return musicBlocks.contains(block.getType());
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

		mapConfigurationToFields(config);
		mapConfigurationToFields(config.getConfigurationSection("messages"));

		ConfigurationSection baseBlocks = config.getConfigurationSection("baseBlocks");
		for (String block : Objects.requireNonNull(baseBlocks).getKeys(false)) {
			Material material = Material.valueOf(block);
			blockSpeeds.put(material, baseBlocks.getDouble(block + ".speed"));
			if (baseBlocks.contains(block + ".music") && baseBlocks.getBoolean(block + ".music")) {
				musicBlocks.add(material);
			}
		}

		BiPredicate<List<String>, Material> anyMaterialMatch = (list, mat) -> list.stream()
				.anyMatch(configMat -> mat.name()
						.matches(configMat.toUpperCase()
								.replace("*", ".*?")));

		List<String> configFloorMaterials = config.getStringList("floorBlocks");
		Arrays.stream(Material.values())
				.filter(material -> anyMaterialMatch.test(configFloorMaterials, material))
				.forEach(floorMaterials::add);
		plugin.logDebug("Floor materials added: " + floorMaterials);

		List<String> configButtonMaterials = config.getStringList("buttonBlocks");
		Arrays.stream(Material.values())
				.filter(material -> anyMaterialMatch.test(configButtonMaterials, material))
				.forEach(buttonMaterials::add);
		plugin.logDebug("Button materials added: " + buttonMaterials);

		List<String> configSignMaterials = config.getStringList("signBlocks");
		Arrays.stream(Material.values())
				.filter(mat -> anyMaterialMatch.test(configSignMaterials, mat))
				.forEach(signMaterials::add);
		plugin.logDebug("Sign materials added: " + signMaterials);

		List<String> configShaftMaterials = config.getStringList("shaftBlocks");
		Arrays.stream(Material.values())
				.filter(mat -> anyMaterialMatch.test(configShaftMaterials, mat))
				.forEach(shaftBlocks::add);
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
	}

	private void mapConfigurationToFields(ConfigurationSection section) {
		if (section == null) {
			return;
		}
		Class<? extends Config> clazz = Config.class;
		Map<String, Field> classFields = Arrays.stream(clazz.getDeclaredFields())
				.collect(Collectors.toMap(Field::getName, field -> field));

		section.getKeys(false)
				.stream()
				.filter(classFields::containsKey)
				.forEach(name -> {
					try {
						Field field = classFields.get(name);
						Class<?> fieldType = field.getType();
						Object value = fieldType != String.class ? section.getObject(name, fieldType) : section.getString(name)
								.replace("&", "§");
						field.setAccessible(true);
						field.set(this, value);
					} catch (IllegalAccessException e) {
						throw new ConfigurationException("Error while mapping configuration to class fields", e);
					}
				});
	}

	private static YamlConfiguration getDefaultConfig(LiftPlugin plugin, File defaultConfigFile) {
		copyDefaultConfig(plugin, defaultConfigFile);
		return YamlConfiguration.loadConfiguration(defaultConfigFile);
	}
}
