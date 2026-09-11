package blackdemise.cp.ai;

// Callback for a streamed AI generation: chunks arrive as they are produced, then either
// onComplete (with final usage) or onError fires exactly once to end the stream.
public interface AiStreamHandler {

    void onChunk(String delta);

    void onComplete(AiUsage usage);

    void onError(Throwable error);
}
