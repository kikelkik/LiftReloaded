package com.minecraftcorp.lift.common.model;

import com.minecraftcorp.lift.common.exception.ElevatorChangeException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class FloorSign {

	private static final Messages messages = Messages.INSTANCE;
	public static final String SEPARATOR = ":";
	public static final int LINE_CURRENT_LEVEL = 0;
	public static final int LINE_CURRENT_NAME = 1;
	public static final int LINE_DEST_LEVEL = 2;
	public static final int LINE_DEST_NAME = 3;
	private Elevator elevator;

	public abstract void updateSign(Floor current, Floor dest);

	public abstract boolean isValid();

	public abstract int readDestLevel();

	protected String getLineText(Floor current, Floor dest, int index) {
		switch (index) {
			case LINE_CURRENT_LEVEL:
				return messages.getCurrentFloor() + SEPARATOR + " " + current.getLevel();
			case LINE_CURRENT_NAME:
				return current.getName();
			case LINE_DEST_LEVEL:
				return messages.getDestination() + SEPARATOR + " " + dest.getLevel();
			case LINE_DEST_NAME:
				return dest.getName();
			default:
				throw new ElevatorChangeException("Signs cannot have a line #" + index);
		}
	}
}
