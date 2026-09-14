package blackdemise.cp.chat;

// Coarse classification of the latest user message, used to decide whether to answer or refuse.
public enum ChatIntent {
    GENERAL_CAREER,
    TECHNICAL,
    CV_DISCUSSION,
    INTERVIEW_DISCUSSION,
    OUT_OF_SCOPE
}
