package com.smartjobportal.careerai;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Part;

import jakarta.annotation.PreDestroy;

@Component
public class GoogleGenAiGeminiApiClient implements GeminiApiClient {

	private final String model;
	private final Client client;

	public GoogleGenAiGeminiApiClient(
			@Value("${gemini.api.key:}") String apiKey,
			@Value("${CAREERSYNC_AI_GEMINI_MODEL:gemini-3.8-flash}") String model) {
		String configuredKey = resolveApiKey(apiKey);
		this.model = model == null || model.isBlank() ? "gemini-3.8-flash" : model.trim();
		this.client = configuredKey.isBlank() ? null : Client.builder()
				.apiKey(configuredKey)
				.httpOptions(HttpOptions.builder().timeout(30_000).build())
				.build();
	}

	private static String resolveApiKey(String applicationPropertyKey) {
		if (isUsableApiKey(applicationPropertyKey)) {
			return applicationPropertyKey.trim();
		}
		String environmentKey = System.getenv("GOOGLE_API_KEY");
		return isUsableApiKey(environmentKey) ? environmentKey.trim() : "";
	}

	private static boolean isUsableApiKey(String candidate) {
		if (candidate == null || candidate.isBlank()) {
			return false;
		}
		String normalized = candidate.trim();
		return !normalized.equalsIgnoreCase("YOUR_GEMINI_API_KEY")
				&& !normalized.equalsIgnoreCase("PASTE_YOUR_GEMINI_API_KEY_HERE");
	}

	@Override
	public boolean isConfigured() {
		return client != null;
	}

	@Override
	public String generate(
			String systemPrompt,
			List<CareerAgentAiProvider.CareerAgentTurn> history,
			String message) {
		if (!isConfigured()) {
			throw new CareerAgentProviderUnavailableException(
					"Gemini is not configured. Set GOOGLE_API_KEY in the backend environment.");
		}

		StringBuilder prompt = new StringBuilder();
		if (history != null && !history.isEmpty()) {
			prompt.append("Recent conversation history (oldest first):\n");
			for (CareerAgentAiProvider.CareerAgentTurn turn : history) {
				prompt.append(turn.role().equals("assistant") ? "Assistant: " : "User: ")
						.append(turn.content()).append('\n');
			}
			prompt.append('\n');
		}
		prompt.append("Current user message:\n").append(message);

		try {
			Content instructions = Content.fromParts(Part.fromText(systemPrompt));
			GenerateContentConfig config = GenerateContentConfig.builder()
					.systemInstruction(instructions)
					.maxOutputTokens(1024)
					.build();
			GenerateContentResponse response = client.models.generateContent(model, prompt.toString(), config);
			String answer = response == null ? "" : response.text();
			if (answer == null || answer.isBlank()) {
				throw new CareerAgentProviderUnavailableException("Gemini returned an empty response.");
			}
			return answer.trim();
		} catch (CareerAgentProviderUnavailableException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new CareerAgentProviderUnavailableException("Gemini could not generate a response.", exception);
		}
	}

	@PreDestroy
	void closeClient() {
		if (client != null) {
			client.close();
		}
	}
}
