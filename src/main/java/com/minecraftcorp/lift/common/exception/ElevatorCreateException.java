package com.minecraftcorp.lift.common.exception;

public class ElevatorCreateException extends ElevatorException {

	private static final String DEFAULT_MESSAGE = "An Exception occurred while creating an elevator!";

	public ElevatorCreateException(String message) {
		super(message);
	}

	public ElevatorCreateException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
