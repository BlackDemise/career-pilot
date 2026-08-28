package blackdemise.cp.chat.dto;

import java.time.Instant;
import java.util.UUID;

import blackdemise.cp.chat.MessageRole;

public record MessageResponse(UUID id, MessageRole role, String content, Instant createdAt) {
}