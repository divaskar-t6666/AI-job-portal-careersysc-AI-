package com.smartjobportal.careerai;

import java.util.List;

public interface GeminiApiClient {

	boolean isConfigured();

	String generate(String systemPrompt, List<CareerAgentAiProvider.CareerAgentTurn> history, String message);
}
