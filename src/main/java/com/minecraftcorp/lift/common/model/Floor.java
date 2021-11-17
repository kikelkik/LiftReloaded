package com.minecraftcorp.lift.common.model;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Floor implements Comparable<Floor> {

	protected final int level;
	protected final String name;
	protected final int buttonY;
	protected final List<FloorSign> signs;

	public int getFloorY() {
		return buttonY - 2;
	}

	public void updateSigns(Floor destination) {
		signs.forEach(sign -> sign.updateSign(this, destination));
	}

	@Override
	public int compareTo(Floor other) {
		if (level == other.getLevel()) return 0;
		return level > other.getLevel() ? 1 : -1;
	}
}
