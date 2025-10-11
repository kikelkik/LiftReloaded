package io.liftreloaded;

import io.liftreloaded.spigot.listener.SpigotPlayerListener;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class LiftPlugin extends JavaPlugin {

	@Override
	public void onEnable() {
		io.liftreloaded.bukkit.LiftPlugin.INSTANCE.enablePlugin(this, SpigotPlayerListener::new);
	}
}
