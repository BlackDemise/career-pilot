package blackdemise.cp.ai;

import org.springframework.stereotype.Service;

import com.google.genai.ResponseStream;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerateContentResponseUsageMetadata;
import com.google.genai.types.Part;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final GeminiClient geminiClient;

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        Content systemInstruction = systemInstruction(systemPrompt);
        Content userContent = Content.fromParts(Part.fromText(userPrompt));

        GenerateContentResponse response = geminiClient.generateContent(systemInstruction, userContent);

        return extractText(response);
    }

    @Override
    public void generateStream(String systemPrompt, String userPrompt, AiStreamHandler handler) {
        Content systemInstruction = systemInstruction(systemPrompt);
        Content userContent = Content.fromParts(Part.fromText(userPrompt));

        StringBuilder accumulated = new StringBuilder();
        try (ResponseStream<GenerateContentResponse> stream = geminiClient.generateContentStream(systemInstruction, userContent)) {
            AiUsage usage = new AiUsage(0, 0, 0);
            for (GenerateContentResponse response : stream) {
                String delta = response.text();
                if (delta != null && !delta.isEmpty()) {
                    accumulated.append(delta);
                    handler.onChunk(delta);
                }
                usage = extractUsage(response, usage);
            }
            if (accumulated.isEmpty()) {
                throw new AiServiceException("Gemini API returned an empty response");
            }
            handler.onComplete(usage);
        } catch (RuntimeException ex) {
            handler.onError(ex instanceof AiServiceException ? ex : new AiServiceException("Gemini API streaming failed", ex));
        }
    }

    private Content systemInstruction(String systemPrompt) {
        return (systemPrompt == null || systemPrompt.isBlank())
                ? null
                : Content.fromParts(Part.fromText(systemPrompt));
    }

    private String extractText(GenerateContentResponse response) {
        String text = response == null ? null : response.text();
        if (text == null || text.isBlank()) {
            throw new AiServiceException("Gemini API returned an empty response");
        }
        return text;
    }

    private AiUsage extractUsage(GenerateContentResponse response, AiUsage previous) {
        return response.usageMetadata()
                .map(metadata -> toUsage(metadata))
                .orElse(previous);
    }

    private AiUsage toUsage(GenerateContentResponseUsageMetadata metadata) {
        return new AiUsage(
                metadata.promptTokenCount().orElse(0),
                metadata.candidatesTokenCount().orElse(0),
                metadata.totalTokenCount().orElse(0));
    }
}

