package com.minecraftcorp.lift.bukkit.service;

import com.minecraftcorp.lift.bukkit.LiftPlugin;
import com.minecraftcorp.lift.bukkit.model.BukkitConfig;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

@UtilityClass
public class NoCheatModifier {

	private static final BukkitConfig config = BukkitConfig.INSTANCE;
	private static final LiftPlugin plugin = LiftPlugin.INSTANCE;
	public static final FixedMetadataValue CAN_FLY_METADATA_TRUE = new FixedMetadataValue(plugin, true);
	public static final FixedMetadataValue CAN_FLY_METADATA_FALSE = new FixedMetadataValue(plugin, false);
	public static final String CAN_FLY_KEY = "lift_canFly";

	public static void setFlightRules(Player player) {
		player.setMetadata(CAN_FLY_KEY, player.getAllowFlight() ? CAN_FLY_METADATA_TRUE : CAN_FLY_METADATA_FALSE);
		player.setAllowFlight(true);
		if (config.isUseNoCheatPlus()) {
			NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_NOFALL);
			NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_SURVIVALFLY);
		}
	}

	public static void resetFlightRules(Player player) {
		List<MetadataValue> metadata = player.getMetadata(CAN_FLY_KEY);
		if (metadata.isEmpty()) {
			plugin.logWarn("Metadata " + CAN_FLY_KEY + " not found on player " + player.getName());
			return;
		}
		if (metadata.size() > 1) {
			plugin.logWarn("Player " + player.getName() + " has too many metadata for " + CAN_FLY_KEY);
		}
		player.setAllowFlight(metadata.get(0).asBoolean());
		player.removeMetadata(CAN_FLY_KEY, plugin);
		if (config.isUseNoCheatPlus()) {
			NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_NOFALL);
			NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_SURVIVALFLY);
		}
	}
}
