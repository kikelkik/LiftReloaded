package com.minecraftcorp.lift.bukkit.model;

import com.minecraftcorp.lift.common.exception.ElevatorChangeException;
import com.minecraftcorp.lift.common.exception.ElevatorException;
import com.minecraftcorp.lift.common.model.Floor;
import com.minecraftcorp.lift.common.model.FloorSign;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;

@Getter
@RequiredArgsConstructor
public class BukkitFloorSign extends FloorSign {

	public static final Side DEFAULT_SIDE = Side.FRONT;
	private final Sign sign;

	@Override
	public void updateSign(Floor current, Floor dest) {
		SignSide side = sign.getSide(DEFAULT_SIDE);
		side.setLine(LINE_CURRENT_LEVEL, getLineText(current, dest, LINE_CURRENT_LEVEL));
		side.setLine(LINE_CURRENT_NAME, getLineText(current, dest, LINE_CURRENT_NAME));
		side.setLine(LINE_DEST_LEVEL, getLineText(current, dest, LINE_DEST_LEVEL));
		side.setLine(LINE_DEST_NAME, getLineText(current, dest, LINE_DEST_NAME));
		if(!sign.update()) {
			throw new ElevatorChangeException("Could not update sign of elevator floor");
		}
	}

	@Override
	public boolean isValid() {
		try {
			return !sign.getSide(DEFAULT_SIDE)
					.getLine(LINE_CURRENT_LEVEL)
					.isEmpty() && readDestLevel() >= 0;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public int readDestLevel() {
		String line = sign.getSide(DEFAULT_SIDE)
				.getLine(LINE_DEST_LEVEL);
		if (!line.contains(SEPARATOR)) {
			throw new ElevatorException("Sign does not contain '" + SEPARATOR + "' on line " + LINE_DEST_LEVEL);
		}
		try {
			return Integer.parseInt(line.split(SEPARATOR)[1].trim());
		} catch (NumberFormatException e) {
			throw new ElevatorException("Sign does not have the correct format");
		}
	}

	@Override
	public String readCurrentName() {
		return sign.getSide(DEFAULT_SIDE)
				.getLine(LINE_CURRENT_NAME);
	}
}
