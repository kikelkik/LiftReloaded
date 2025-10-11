package io.liftreloaded;

import io.liftreloaded.paper.listener.PaperPlayerListener;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class LiftPlugin extends JavaPlugin {

	@Override
	public void onEnable() {
		io.liftreloaded.bukkit.LiftPlugin.INSTANCE.enablePlugin(this, PaperPlayerListener::new);
	}
}
