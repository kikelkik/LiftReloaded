package com.minecraftcorp.lift.common.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Calculator {

	public static int compareBlockCoords(int x1, int z1, int x2, int z2) {
		if (x1 + z1 == x2 + z2) return 0;
		return x1 + z1 > x2 + z2 ? 1 : -1;
	}

	public boolean isScrollForwards(int newSlot, int previousSlot) {
		if (previousSlot < newSlot) {
			return previousSlot != 0 && newSlot != 8;
		}
		return previousSlot != 8 && newSlot != 0;
	}
}
