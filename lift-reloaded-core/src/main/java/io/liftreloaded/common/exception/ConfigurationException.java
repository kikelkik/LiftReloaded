package io.liftreloaded.common.exception;

public class ConfigurationException extends ElevatorException {

	private static final String DEFAULT_MESSAGE = "Configuration error!";

	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConfigurationException(String message) {
		super(message);
	}

	public ConfigurationException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
