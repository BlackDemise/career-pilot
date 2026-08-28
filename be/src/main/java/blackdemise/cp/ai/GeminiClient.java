package blackdemise.cp.ai;

import org.springframework.stereotype.Component;

import com.google.genai.Client;
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
        GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder()
                .temperature((float) properties.temperature())
                .maxOutputTokens(properties.maxOutputTokens());
        if (systemInstruction != null) {
            configBuilder.systemInstruction(systemInstruction);
        }

        try {
            return client.models.generateContent(properties.model(), userContent, configBuilder.build());
        } catch (GenAiIOException ex) {
            throw new AiServiceException("Gemini API request timed out or was unreachable", ex);
        } catch (RuntimeException ex) {
            throw new AiServiceException("Gemini API call failed", ex);
        }
    }
}
