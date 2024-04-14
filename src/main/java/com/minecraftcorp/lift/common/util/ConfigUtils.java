package com.minecraftcorp.lift.common.util;

import com.minecraftcorp.lift.bukkit.LiftPlugin;
import com.minecraftcorp.lift.common.exception.ConfigurationException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

@UtilityClass
public class ConfigUtils {

	public void mapConfigurationToFields(Set<String> configKeys, Object object,
			BiFunction<String, Class<?>, Object> valueResolver, Class<?> clazz) {

		Map<String, Field> classFields = Arrays.stream(clazz.getDeclaredFields())
				.collect(Collectors.toMap(Field::getName, field -> field));

		configKeys.stream()
				.filter(classFields::containsKey)
				.forEach(name -> {
					try {
						Field field = classFields.get(name);
						field.setAccessible(true);
						field.set(object, valueResolver.apply(name, field.getType()));
					} catch (IllegalAccessException e) {
						throw new ConfigurationException("Error while mapping configuration to class fields", e);
					}
				});
	}

	public static void migrateConfig(LiftPlugin plugin, YamlConfiguration config) {
		Configuration defaultConfig = Objects.requireNonNull(config.getDefaults());
		int currentVersion = defaultConfig.getInt("configVersion");
		int version = config.getInt("configVersion", 1);
		if (version == currentVersion) {
			return; // nothing to migrate
		}
		if (version < 2) {
			plugin.logInfo("Detected old config version " + version + ". Will migrate config to " + currentVersion);
			migrateBaseBlocksConfig(config, defaultConfig);
		}
		config.set("configVersion", currentVersion);
	}

	private static void migrateBaseBlocksConfig(YamlConfiguration config, Configuration defaultConfig) {
		// baseBlocks configuration structure changed: https://github.com/kikelkik/LiftReloaded/issues/26
		ConfigurationSection baseBlocks = config.getConfigurationSection("baseBlocks");
		if (baseBlocks != null) {
			List<Map<String, Object>> migrated = new ArrayList<>();
			baseBlocks.getKeys(false).forEach(block -> {
				ConfigurationSection baseBlockConfig = Objects.requireNonNull(baseBlocks.getConfigurationSection(block));
				Map<String, Object> migratedBlock = new HashMap<>();
				migratedBlock.put("type", block);
				migratedBlock.put("speed", baseBlockConfig.getDouble("speed"));
				migratedBlock.put("music", baseBlockConfig.getBoolean("music"));
				migrated.add(migratedBlock);
			});
			if (migrated.isEmpty()) {
				config.set("baseBlocks", defaultConfig.get("baseBlocks"));
			} else {
				config.set("baseBlocks", migrated);
			}
		}
	}
}
