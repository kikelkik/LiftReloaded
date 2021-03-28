package com.minecraftcorp.lift.bukkit.model;

import org.bukkit.block.Sign;

import com.minecraftcorp.lift.common.exception.ElevatorChangeException;
import com.minecraftcorp.lift.common.exception.ElevatorException;
import com.minecraftcorp.lift.common.model.Floor;
import com.minecraftcorp.lift.common.model.FloorSign;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BukkitFloorSign extends FloorSign {

	private final Sign sign;

	@Override
	public void updateSign(Floor current, Floor dest) {
		sign.setLine(LINE_CURRENT_LEVEL, getLineText(current, dest, LINE_CURRENT_LEVEL));
		sign.setLine(LINE_CURRENT_NAME, getLineText(current, dest, LINE_CURRENT_NAME));
		sign.setLine(LINE_DEST_LEVEL, getLineText(current, dest, LINE_DEST_LEVEL));
		sign.setLine(LINE_DEST_NAME, getLineText(current, dest, LINE_DEST_NAME));
		if(!sign.update()) {
			throw new ElevatorChangeException("Could not update sign of elevator floor");
		}
	}

	@Override
	public boolean isEmpty() {
		return sign.getLine(LINE_CURRENT_LEVEL).isEmpty();
	}

	@Override
	public int readDestLevel() {
		String line = sign.getLine(LINE_DEST_LEVEL);
		if (!line.contains(SEPARATOR)) {
			throw new ElevatorException("Sign is empty");
		}
		try {
			return Integer.parseInt(line.split(SEPARATOR)[1].trim());
		} catch (NumberFormatException e) {
			throw new ElevatorException("Sign does not have the correct format");
		}
	}
}
