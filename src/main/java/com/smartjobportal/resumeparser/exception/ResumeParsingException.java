package com.smartjobportal.resumeparser.exception;

public class ResumeParsingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ResumeParsingException(String message) {
		super(message);
	}

	public ResumeParsingException(String message, Throwable cause) {
		super(message, cause);
	}
}
