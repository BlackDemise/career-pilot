package blackdemise.cp.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.google.genai.ResponseStream;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerateContentResponseUsageMetadata;

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

    @SuppressWarnings("unchecked")
    @Test
    void generateStream_emitsChunksThenCompletesWithUsage() {
        GenerateContentResponse chunk1 = mock(GenerateContentResponse.class);
        when(chunk1.text()).thenReturn("Hello");
        when(chunk1.usageMetadata()).thenReturn(Optional.empty());

        GenerateContentResponse chunk2 = mock(GenerateContentResponse.class);
        when(chunk2.text()).thenReturn(" world");
        GenerateContentResponseUsageMetadata usageMetadata = mock(GenerateContentResponseUsageMetadata.class);
        when(usageMetadata.promptTokenCount()).thenReturn(Optional.of(10));
        when(usageMetadata.candidatesTokenCount()).thenReturn(Optional.of(5));
        when(usageMetadata.totalTokenCount()).thenReturn(Optional.of(15));
        when(chunk2.usageMetadata()).thenReturn(Optional.of(usageMetadata));

        ResponseStream<GenerateContentResponse> stream = mock(ResponseStream.class);
        when(stream.iterator()).thenReturn(List.of(chunk1, chunk2).iterator());
        when(geminiClient.generateContentStream(any(), any())).thenReturn(stream);

        List<String> chunks = new java.util.ArrayList<>();
        AtomicReference<AiUsage> usageRef = new AtomicReference<>();
        aiService.generateStream("system prompt", "user prompt", new AiStreamHandler() {
            @Override
            public void onChunk(String delta) {
                chunks.add(delta);
            }

            @Override
            public void onComplete(AiUsage usage) {
                usageRef.set(usage);
            }

            @Override
            public void onError(Throwable error) {
                fail("unexpected error: " + error.getMessage());
            }
        });

        assertThat(chunks).containsExactly("Hello", " world");
        assertThat(usageRef.get()).isEqualTo(new AiUsage(10, 5, 15));
        verify(stream).close();
    }

    @SuppressWarnings("unchecked")
    @Test
    void generateStream_reportsError_whenStreamingFails() {
        ResponseStream<GenerateContentResponse> stream = mock(ResponseStream.class);
        when(stream.iterator()).thenThrow(new RuntimeException("boom"));
        when(geminiClient.generateContentStream(any(), any())).thenReturn(stream);

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        aiService.generateStream(null, "user prompt", new AiStreamHandler() {
            @Override
            public void onChunk(String delta) {
                fail("no chunk expected");
            }

            @Override
            public void onComplete(AiUsage usage) {
                fail("no completion expected");
            }

            @Override
            public void onError(Throwable error) {
                errorRef.set(error);
            }
        });

        assertThat(errorRef.get()).isInstanceOf(AiServiceException.class);
    }
}

