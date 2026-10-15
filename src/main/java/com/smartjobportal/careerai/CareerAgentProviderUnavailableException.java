package com.smartjobportal.careerai;

public class CareerAgentProviderUnavailableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CareerAgentProviderUnavailableException(String message) {
		super(message);
	}

	public CareerAgentProviderUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
