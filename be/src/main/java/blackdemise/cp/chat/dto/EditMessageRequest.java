package blackdemise.cp.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditMessageRequest(
        @NotBlank(message = "Content is required")
        @Size(max = 20000, message = "Content must not exceed 20000 characters") String content) {
}
