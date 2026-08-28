package blackdemise.cp.ai;

import org.springframework.stereotype.Service;

import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final GeminiClient geminiClient;

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        Content systemInstruction = (systemPrompt == null || systemPrompt.isBlank())
                ? null
                : Content.fromParts(Part.fromText(systemPrompt));
        Content userContent = Content.fromParts(Part.fromText(userPrompt));

        GenerateContentResponse response = geminiClient.generateContent(systemInstruction, userContent);

        return extractText(response);
    }

    private String extractText(GenerateContentResponse response) {
        String text = response == null ? null : response.text();
        if (text == null || text.isBlank()) {
            throw new AiServiceException("Gemini API returned an empty response");
        }
        return text;
    }
}
