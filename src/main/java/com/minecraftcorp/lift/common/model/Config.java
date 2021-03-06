package com.minecraftcorp.lift.common.model;

import com.minecraftcorp.lift.bukkit.LiftPlugin;
import com.minecraftcorp.lift.common.exception.ConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.Getter;

/**
 * Represents the config file
 * See Documentation of derived classes
 */
@Getter
public abstract class Config {

	protected Boolean debug;
	protected Integer maxLiftArea;
	protected Integer maxHeight;
	protected Integer minHeight;
	protected Boolean autoPlace;
	protected Boolean serverFlight;
	protected Boolean liftMobs;
	protected Boolean preventEntry;
	protected Boolean preventLeave;
	protected Boolean mouseScroll;
	protected Integer secondsUntilTimeout;
	protected Boolean soundEnabled;
	protected Integer soundVolume;

	public float relativeVolume(double volume) {
		return (float) (volume * soundVolume / 100.0);
	}

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

	protected void validate() throws ConfigurationException {
		if (soundVolume < 0 || soundVolume > 100) {
			throw new ConfigurationException("soundVolume must have a value from 0 to 100");
		}
	}
}