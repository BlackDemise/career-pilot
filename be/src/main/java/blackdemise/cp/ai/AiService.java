package blackdemise.cp.ai;

import java.util.List;

// Single entry point every feature must use to talk to the AI model; never call Gemini directly.
public interface AiService {

    /**
     * @param systemPrompt system-level instructions, may be null/blank for none
     * @param userPrompt   the fully-rendered user/task prompt
     * @return the model's raw text response
     */
    String generate(String systemPrompt, String userPrompt);

    /**
     * Streams the model's response as it is generated. {@code handler} receives text deltas via
     * {@code onChunk}, then exactly one of {@code onComplete} (with final token usage) or
     * {@code onError}. Retries are only attempted before the first chunk is emitted; once
     * streaming has started, a failure is reported through {@code onError}.
     *
     * @param systemPrompt system-level instructions, may be null/blank for none
     * @param userPrompt   the fully-rendered user/task prompt
     */
    void generateStream(String systemPrompt, String userPrompt, AiStreamHandler handler);

    /**
     * Classifies {@code userPrompt} into exactly one of {@code allowedValues}, using structured
     * (enum-constrained) output rather than free text. Intended for cheap, deterministic
     * decisions (e.g. intent classification), not for user-facing generation.
     *
     * @param systemPrompt  system-level instructions, may be null/blank for none
     * @param userPrompt    the fully-rendered classification prompt
     * @param allowedValues the exact set of values the model may return
     * @return one of {@code allowedValues}
     */
    String classify(String systemPrompt, String userPrompt, List<String> allowedValues);
}

