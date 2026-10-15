package com.smartjobportal.careeragent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartjobportal.careerai.CareerAgentAiProvider;
import com.smartjobportal.careerai.CareerAgentProviderUnavailableException;
import com.smartjobportal.careerai.GeminiApiClient;
import com.smartjobportal.careerai.GeminiCareerAgentProvider;

@ExtendWith(MockitoExtension.class)
class GeminiCareerAgentProviderTest {

	@Mock
	private GeminiApiClient geminiApiClient;

	@Test
	void sendsSystemPromptHistoryAndMessageToMockedGeminiClient() {
		List<CareerAgentAiProvider.CareerAgentTurn> history =
				List.of(new CareerAgentAiProvider.CareerAgentTurn("user", "Hello"));
		when(geminiApiClient.isConfigured()).thenReturn(true);
		when(geminiApiClient.generate("Career guidance rules", history, "Suggest a project"))
				.thenReturn("Build a portfolio project.");
		GeminiCareerAgentProvider provider = new GeminiCareerAgentProvider(geminiApiClient);

		assertTrue(provider.isConfigured());
		assertEquals("Google Gemini", provider.getProviderName());
		assertEquals("Build a portfolio project.",
				provider.generate("Career guidance rules", history, "Suggest a project"));
		verify(geminiApiClient).generate("Career guidance rules", history, "Suggest a project");
	}

	@Test
	void reportsMissingGoogleApiKeyAsUnconfigured() {
		when(geminiApiClient.isConfigured()).thenReturn(false);

		assertFalse(new GeminiCareerAgentProvider(geminiApiClient).isConfigured());
	}

	@Test
	void propagatesSanitizedGeminiApiErrors() {
		when(geminiApiClient.generate("Rules", List.of(), "Hello"))
				.thenThrow(new IllegalStateException("upstream request failed"));

		CareerAgentProviderUnavailableException exception = assertThrows(
				CareerAgentProviderUnavailableException.class,
				() -> new GeminiCareerAgentProvider(geminiApiClient).generate("Rules", List.of(), "Hello"));

		assertEquals("Gemini could not generate a response.", exception.getMessage());
	}
}
