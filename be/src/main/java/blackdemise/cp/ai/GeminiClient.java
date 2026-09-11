package blackdemise.cp.ai;

import org.springframework.stereotype.Component;

import com.google.genai.Client;
import com.google.genai.ResponseStream;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;

// Thin wrapper around the official Google Gen AI Java SDK (com.google.genai:google-genai).
@Component
public class GeminiClient {

    private final Client client;
    private final AiProperties properties;

    public GeminiClient(AiProperties properties) {
        this.properties = properties;
        this.client = Client.builder()
                .apiKey(properties.apiKey())
                .httpOptions(HttpOptions.builder().timeout((int) properties.timeoutMs()).build())
                .build();
    }

    public GenerateContentResponse generateContent(Content systemInstruction, Content userContent) {
        GenerateContentConfig config = buildConfig(systemInstruction);
        try {
            return client.models.generateContent(properties.model(), userContent, config);
        } catch (GenAiIOException ex) {
            throw new AiServiceException("Gemini API request timed out or was unreachable", ex);
        } catch (RuntimeException ex) {
            throw new AiServiceException("Gemini API call failed", ex);
        }
    }

    // Retries only opening the stream (transient timeouts/connection errors). Once chunks have
    // started arriving, failures are surfaced to the caller instead of silently retrying.
    public ResponseStream<GenerateContentResponse> generateContentStream(Content systemInstruction, Content userContent) {
        GenerateContentConfig config = buildConfig(systemInstruction);
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return client.models.generateContentStream(properties.model(), userContent, config);
            } catch (GenAiIOException ex) {
                if (attempt > properties.maxRetries()) {
                    throw new AiServiceException("Gemini API request timed out or was unreachable", ex);
                }
                sleepBeforeRetry(attempt);
            } catch (RuntimeException ex) {
                throw new AiServiceException("Gemini API call failed", ex);
            }
        }
    }

    private GenerateContentConfig buildConfig(Content systemInstruction) {
        GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder()
                .temperature((float) properties.temperature())
                .maxOutputTokens(properties.maxOutputTokens());
        if (systemInstruction != null) {
            configBuilder.systemInstruction(systemInstruction);
        }
        return configBuilder.build();
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(properties.retryBackoffMs() * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("Interrupted while retrying Gemini API call", interrupted);
        }
    }
}

