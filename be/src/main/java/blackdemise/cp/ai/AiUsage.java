package blackdemise.cp.ai;

// Token usage reported by Gemini for a single generation call.
public record AiUsage(int promptTokens, int completionTokens, int totalTokens) {
}
