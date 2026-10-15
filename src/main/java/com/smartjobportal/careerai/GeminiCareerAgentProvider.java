package com.smartjobportal.careerai;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class GeminiCareerAgentProvider implements CareerAgentAiProvider {

	private final GeminiApiClient geminiApiClient;

	public GeminiCareerAgentProvider(GeminiApiClient geminiApiClient) {
		this.geminiApiClient = geminiApiClient;
	}

	@Override
	public boolean isConfigured() {
		return geminiApiClient.isConfigured();
	}

	@Override
	public String getProviderName() {
		return "Google Gemini";
	}

	@Override
	public String generate(String systemPrompt, List<CareerAgentTurn> history, String message) {
		try {
			return geminiApiClient.generate(systemPrompt, history, message);
		} catch (CareerAgentProviderUnavailableException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new CareerAgentProviderUnavailableException("Gemini could not generate a response.", exception);
		}
	}
}
