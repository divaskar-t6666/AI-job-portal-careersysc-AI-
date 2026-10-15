
package com.smartjobportal.careerai;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.google.genai.errors.ApiException;

class GeminiLiveDiagnosticTest {

    @Test
    @EnabledIfSystemProperty(
        named = "gemini.live-diagnostic",
        matches = "true"
    )
    void reportsGeminiLiveStatusWithoutExposingSecrets() {
        String apiKey = configuredApiKey();

        if (apiKey.isBlank()) {
            System.out.println("GEMINI_DIAGNOSTIC=KEY_MISSING");
            return;
        }

        String model = System.getenv("CAREERSYNC_AI_GEMINI_MODEL");
        if (model != null && model.isBlank()) {
            model = null;
        } else if (model != null) {
            model = model.trim();
        }

        long startTime = System.nanoTime();
        GoogleGenAiGeminiApiClient client = null;

        try {
            client = new GoogleGenAiGeminiApiClient(apiKey, model);

            System.out.println("GEMINI_DIAGNOSTIC=REQUEST_STARTED");

            client.generate(
                "Reply with one word only.",
                List.of(),
                "Reply: OK"
            );

            System.out.println("GEMINI_DIAGNOSTIC=SUCCESS");

        } catch (Exception exception) {
            System.out.println(
                "GEMINI_DIAGNOSTIC=" + classify(exception)
            );

        } finally {
            if (client != null) {
                try {
                    client.closeClient();
                } catch (Exception ignored) {
                    System.out.println(
                        "GEMINI_DIAGNOSTIC_CLEANUP=WARNING"
                    );
                }
            }

            long durationMs = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startTime
            );

            System.out.println(
                "GEMINI_DIAGNOSTIC_DURATION_MS=" + durationMs
            );
        }
    }

    private String configuredApiKey() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
            // Fall back to the environment variable; never print configuration values.
        }

        String propertyKey = properties.getProperty("gemini.api.key", "").trim();
        if (isUsableKey(propertyKey)) {
            return propertyKey;
        }

        String environmentKey = System.getenv("GOOGLE_API_KEY");
        return isUsableKey(environmentKey) ? environmentKey.trim() : "";
    }

    private boolean isUsableKey(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        String normalized = candidate.trim();
        return !normalized.equalsIgnoreCase("YOUR_GEMINI_API_KEY")
            && !normalized.equalsIgnoreCase("PASTE_YOUR_GEMINI_API_KEY_HERE")
            && !(normalized.startsWith("${") && normalized.endsWith("}"));
    }

    private String classify(Throwable exception) {
        for (
            Throwable cause = exception;
            cause != null;
            cause = cause.getCause()
        ) {
            if (cause instanceof ApiException apiException) {
                int status = apiException.code();

                String statusText = apiException.status() == null
                    ? ""
                    : apiException.status();

                String message = apiException.message() == null
                    ? ""
                    : apiException.message();
                String lowerMessage = message.toLowerCase(java.util.Locale.ROOT);

                if (status == 401
                    || statusText.contains("API_KEY_INVALID")
                    || lowerMessage.contains("api key not valid")
                    || lowerMessage.contains("invalid api key")) {
                    return "INVALID_KEY_HTTP_" + status;
                }

                if (status == 403) {
                    return "PERMISSION_ERROR_HTTP_403";
                }

                if (status == 429) {
                    return "QUOTA_ERROR_HTTP_429";
                }

                if (status == 400 || status == 404) {
                    return "MODEL_OR_REQUEST_ERROR_HTTP_" + status;
                }

                return "OTHER_API_ERROR_HTTP_" + status;
            }
        }

        return "OTHER_API_ERROR";
    }
}
