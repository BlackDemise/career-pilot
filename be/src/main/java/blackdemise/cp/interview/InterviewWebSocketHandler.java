package blackdemise.cp.interview;

import java.net.URI;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import blackdemise.cp.security.jwt.JwtTokenProvider;
import blackdemise.cp.security.jwt.TokenBlacklistService;
import blackdemise.cp.interview.dto.SubmitAnswerRequest;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class InterviewWebSocketHandler extends TextWebSocketHandler implements WebSocketHandler {
    private final InterviewService interviewService;
    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistService blacklistService;
    private final JsonMapper jsonMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession socket) throws Exception {
        try {
            String token = queryValue(socket.getUri(), "accessToken");
            var claims = tokenProvider.parse(token).getPayload();
            if (!"ACCESS".equals(claims.get("typ", String.class)) || blacklistService.isBlacklisted(claims.getId())) {
                socket.close();
                return;
            }
            UUID userId = UUID.fromString(claims.getSubject());
            UUID sessionId = UUID.fromString(pathValue(socket.getUri()));
            socket.getAttributes().put("userId", userId);
            socket.getAttributes().put("sessionId", sessionId);
            var started = interviewService.start(userId, sessionId);
            socket.sendMessage(new TextMessage(jsonMapper.writeValueAsString(
                    new InterviewSocketEvent("SESSION_STARTED", started))));
            socket.sendMessage(new TextMessage(jsonMapper.writeValueAsString(
                    new InterviewSocketEvent("PHASE_CHANGED", started))));
        } catch (RuntimeException ex) {
            socket.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession socket, TextMessage message) throws Exception {
        UUID userId = (UUID) socket.getAttributes().get("userId");
        UUID sessionId = (UUID) socket.getAttributes().get("sessionId");
        var payload = jsonMapper.readValue(message.getPayload(), InterviewSocketMessage.class);
        if ("ANSWER_SUBMITTED".equals(payload.type())) {
            interviewService.submitAnswer(userId, sessionId, payload.questionId(), payload.answer());
            sendNextOrCompleted(socket, userId, sessionId);
        } else if ("ANSWER_TIMEOUT".equals(payload.type())) {
            interviewService.timeout(userId, sessionId, payload.questionId());
            sendNextOrCompleted(socket, userId, sessionId);
        } else if ("INTEGRITY_EVENT".equals(payload.type())) {
            interviewService.recordIntegrityEvent(userId, sessionId, payload.eventType(), payload.metadata());
        }
    }

    private void sendNextOrCompleted(WebSocketSession socket, UUID userId, UUID sessionId) throws Exception {
        var session = interviewService.get(userId, sessionId);
        if (session.status() == InterviewStatus.COMPLETED) {
            socket.sendMessage(new TextMessage(jsonMapper.writeValueAsString(new InterviewSocketEvent("PHASE_CHANGED", session))));
            socket.sendMessage(new TextMessage(jsonMapper.writeValueAsString(new InterviewSocketEvent("INTERVIEW_COMPLETED", session))));
        } else {
            var nextQuestion = interviewService.start(userId, sessionId);
            socket.sendMessage(new TextMessage(jsonMapper.writeValueAsString(new InterviewSocketEvent("PHASE_CHANGED", session))));
            socket.sendMessage(new TextMessage(jsonMapper.writeValueAsString(new InterviewSocketEvent("INTERVIEWER_MESSAGE",
                    nextQuestion))));
        }
    }

    private String queryValue(URI uri, String key) {
        if (uri == null || uri.getQuery() == null) throw new IllegalArgumentException("Missing token");
        for (String pair : uri.getQuery().split("&")) {
            String[] values = pair.split("=", 2);
            if (values.length == 2 && key.equals(values[0])) return values[1];
        }
        throw new IllegalArgumentException("Missing token");
    }

    private String pathValue(URI uri) {
        String[] parts = uri.getPath().split("/");
        return parts[parts.length - 2];
    }

    public record InterviewSocketMessage(String type, UUID questionId, SubmitAnswerRequest answer,
            String eventType, String metadata) { }
    public record InterviewSocketEvent(String type, Object payload) { }
}