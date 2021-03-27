package com.minecraftcorp.lift.common.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Floor implements Comparable<Floor> {

	protected final int level;
	protected final String name;
	protected final int buttonY;
	protected final FloorSign sign;

	public int getFloorY() {
		return buttonY - 2;
	}

	public void updateSign(Floor destination) {
		sign.updateSign(this, destination);
	}

	@Override
	public int compareTo(Floor other) {
		if (level == other.getLevel()) return 0;
		return level > other.getLevel() ? 1 : -1;
	}
}
