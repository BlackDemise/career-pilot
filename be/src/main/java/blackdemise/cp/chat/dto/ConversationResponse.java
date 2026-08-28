package blackdemise.cp.chat.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationResponse(
        UUID id, String title, Instant createdAt, Instant updatedAt, List<MessageResponse> messages) {
}