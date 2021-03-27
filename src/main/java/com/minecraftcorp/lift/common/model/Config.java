package com.minecraftcorp.lift.common.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.minecraftcorp.lift.bukkit.LiftPlugin;

import lombok.Getter;

/**
 * Represents the config file
 * See Documentation of derived classes
 */
@Getter
public abstract class Config {

	protected Boolean debug;
	protected Boolean redstone;
	protected Integer maxLiftArea;
	protected Integer maxHeight;
	protected Boolean autoPlace;
	protected Boolean checkFloor;
	protected Boolean serverFlight;
	protected Boolean liftMobs;
	protected Boolean preventEntry;
	protected Boolean preventLeave;
	protected Boolean mouseScroll;
	protected String destination;
	protected String currentFloor;
	protected String oneFloor;
	protected String cantEnter;
	protected String cantLeave;
	protected String unsafe;
	protected String scrollSelectEnabled;
	protected String scrollSelectDisabled;
	protected Integer secondsUntilTimeout;

	protected static void copyDefaultConfig(LiftPlugin plugin, File dest) {
		try (InputStream in = plugin.getResource("config.yml")) {
			if (in == null) {
				throw new IOException("Error while preparing copy of default config");
			}
			Path destPath = dest.toPath();
			Files.createDirectories(destPath.getParent());
			Files.deleteIfExists(destPath);
			Files.createFile(destPath);
			Files.copy(in, destPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException("Lift could not copy default config file!", e);
		}
	}

	protected static void deleteDefaultConfig(LiftPlugin plugin, File defaultConfigFile) {
		try {
			Files.deleteIfExists(defaultConfigFile.toPath());
			Files.deleteIfExists(defaultConfigFile.toPath()
					.getParent());
		} catch (Exception e) {
			plugin.logWarn("Could not delete default directory");
		}
	}
}