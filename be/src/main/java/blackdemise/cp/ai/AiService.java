package blackdemise.cp.ai;

// Single entry point every feature must use to talk to the AI model; never call Gemini directly.
public interface AiService {

    /**
     * @param systemPrompt system-level instructions, may be null/blank for none
     * @param userPrompt   the fully-rendered user/task prompt
     * @return the model's raw text response
     */
    String generate(String systemPrompt, String userPrompt);
}
