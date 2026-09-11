package blackdemise.cp.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import blackdemise.cp.chat.dto.ConversationResponse;
import blackdemise.cp.chat.dto.CreateConversationRequest;
import blackdemise.cp.chat.dto.EditMessageRequest;
import blackdemise.cp.chat.dto.SendMessageRequest;
import blackdemise.cp.common.ApiResponse;
import blackdemise.cp.security.jwt.JwtUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ApiResponse> create(Authentication authentication,
            @Valid @RequestBody CreateConversationRequest request) {
        ConversationResponse result = chatService.create(userId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Conversation created", result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse> list(Authentication authentication) {
        List<ConversationResponse> result = chatService.list(userId(authentication));
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Conversations retrieved", result));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ApiResponse> get(Authentication authentication, @PathVariable UUID conversationId) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Conversation retrieved",
                chatService.get(userId(authentication), conversationId)));
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse> send(Authentication authentication, @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Message sent",
                chatService.send(userId(authentication), conversationId, request)));
    }

    @PostMapping(path = "/{conversationId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendStream(Authentication authentication, @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return chatService.sendStream(userId(authentication), conversationId, request);
    }

    @PostMapping(path = "/{conversationId}/messages/{messageId}/regenerate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter regenerate(Authentication authentication, @PathVariable UUID conversationId,
            @PathVariable UUID messageId) {
        return chatService.regenerateStream(userId(authentication), conversationId, messageId);
    }

    @PostMapping(path = "/{conversationId}/messages/{messageId}/edit", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter edit(Authentication authentication, @PathVariable UUID conversationId,
            @PathVariable UUID messageId, @Valid @RequestBody EditMessageRequest request) {
        return chatService.editAndStream(userId(authentication), conversationId, messageId, request);
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<ApiResponse> delete(Authentication authentication, @PathVariable UUID conversationId) {
        chatService.delete(userId(authentication), conversationId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Conversation deleted", null));
    }

    private UUID userId(Authentication authentication) {
        return ((JwtUserPrincipal) authentication.getPrincipal()).id();
    }
}
