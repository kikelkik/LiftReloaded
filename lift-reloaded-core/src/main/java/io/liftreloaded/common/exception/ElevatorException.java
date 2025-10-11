package io.liftreloaded.common.exception;

public class ElevatorException extends RuntimeException {

	public ElevatorException(String message) {
		super(message);
	}

	public ElevatorException(String message, Throwable cause) {
		super(message, cause);
	}

	public ElevatorException(Throwable cause) {
		super(cause);
	}
}
