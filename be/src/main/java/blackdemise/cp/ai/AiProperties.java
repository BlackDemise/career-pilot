package blackdemise.cp.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Model configuration for the centralized Gemini AI service, sourced from application.yml / env.
@ConfigurationProperties(prefix = "ai.gemini")
public record AiProperties(
        String apiKey,
        String model,
        double temperature,
        int maxOutputTokens,
        long timeoutMs) {
}
