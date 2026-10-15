package com.smartjobportal.careerai;

import java.util.List;

public interface CareerAgentAiProvider {

	boolean isConfigured();

	String getProviderName();

	String generate(String systemPrompt, List<CareerAgentTurn> history, String message);

	record CareerAgentTurn(String role, String content) {
	}
}
