package com.minecraftcorp.lift.common.exception;

public class ElevatorChangeException extends ElevatorException {

	private static final String DEFAULT_MESSAGE = "An Exception occurred by modifying the underlying elevator!";

	public ElevatorChangeException(String message) {
		super(message);
	}

	public ElevatorChangeException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
