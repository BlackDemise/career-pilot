package blackdemise.cp.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;

class AiServiceImplTest {

    private final GeminiClient geminiClient = mock(GeminiClient.class);
    private final AiServiceImpl aiService = new AiServiceImpl(geminiClient);

    @Test
    void generate_returnsModelText_whenGeminiRespondsWithText() {
        GenerateContentResponse response = mock(GenerateContentResponse.class);
        when(response.text()).thenReturn("hello there");
        when(geminiClient.generateContent(any(Content.class), any(Content.class))).thenReturn(response);

        String result = aiService.generate("system prompt", "user prompt");

        assertThat(result).isEqualTo("hello there");
        verify(geminiClient).generateContent(any(Content.class), any(Content.class));
    }

    @Test
    void generate_throwsAiServiceException_whenResponseTextIsBlank() {
        GenerateContentResponse response = mock(GenerateContentResponse.class);
        when(response.text()).thenReturn("");
        when(geminiClient.generateContent(any(), any())).thenReturn(response);

        assertThatThrownBy(() -> aiService.generate(null, "user prompt"))
                .isInstanceOf(AiServiceException.class);
    }
}
