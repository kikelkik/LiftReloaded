package com.minecraftcorp.lift.common.model;

import lombok.Getter;

@Getter
public class Messages {

	public static final Messages INSTANCE = new Messages();

	private String destination;
	private String currentFloor;
	private String oneFloor;
	private String cantEnter;
	private String cantLeave;
	private String unsafe;
	private String scrollSelectEnabled;
	private String scrollSelectDisabled;
	private String floorNotExists;
	private String noBaseBlock;
	private String timeout;
}
