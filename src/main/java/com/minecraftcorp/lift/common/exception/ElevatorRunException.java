package com.minecraftcorp.lift.common.exception;

public class ElevatorRunException extends ElevatorException {

	public ElevatorRunException(String message) {
		super(message);
	}

	public ElevatorRunException(String message, Throwable cause) {
		super(message, cause);
	}

	public ElevatorRunException(Throwable cause) {
		super(cause);
	}
}
