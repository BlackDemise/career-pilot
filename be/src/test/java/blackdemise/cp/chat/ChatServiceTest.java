package blackdemise.cp.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import blackdemise.cp.ai.AiService;
import blackdemise.cp.ai.prompt.PromptTemplateService;
import blackdemise.cp.chat.dto.SendMessageRequest;
import blackdemise.cp.chat.entity.Conversation;
import blackdemise.cp.chat.entity.Message;
import blackdemise.cp.common.exception.NotFoundException;
import blackdemise.cp.user.Role;
import blackdemise.cp.user.User;
import blackdemise.cp.user.UserRepository;

class ChatServiceTest {

    private final ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private final MessageRepository messageRepository = mock(MessageRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AiService aiService = mock(AiService.class);
    private final PromptTemplateService promptTemplateService = mock(PromptTemplateService.class);
    private final ChatService chatService = new ChatService(conversationRepository, messageRepository,
            userRepository, aiService, promptTemplateService);

    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private User user;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(userId);
        user.setPreferredLanguage("English");
        user.setResponseStyle("Concise");
        user.setTechnicalBackground("Java developer");
        user.setCareerGoal("Backend role");
        user.setCustomInstructions("Use practical examples");
        user.setRole(Role.USER);

        conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setUser(user);
        conversation.setTitle("New conversation");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of());
        when(promptTemplateService.render(eq("chat-system"), any(Map.class)))
                .thenReturn("rendered system prompt");
        when(aiService.generate("rendered system prompt", "conversation prompt"))
                .thenReturn("assistant answer");
    }

    @Test
    void send_persistsUserAndAssistantMessagesAndBuildsContext() {
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(message(MessageRole.USER, "How do I prepare?"),
                        message(MessageRole.ASSISTANT, "Practice examples.")));
        when(promptTemplateService.render(eq("chat-system"), any(Map.class)))
                .thenReturn("rendered system prompt");
        when(aiService.generate(eq("rendered system prompt"), any(String.class)))
                .thenReturn("assistant answer");

        chatService.send(userId, conversationId, new SendMessageRequest("  What should I practice?  "));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(aiService).generate(eq("rendered system prompt"), prompt.capture());
        assertThat(prompt.getValue()).contains("USER: How do I prepare?", "ASSISTANT: Practice examples.");
        verify(messageRepository, org.mockito.Mockito.times(2)).save(any(Message.class));
    }

    @Test
    void send_includesAllProfileFieldsInSystemTemplate() {
        when(aiService.generate(eq("rendered system prompt"), any(String.class))).thenReturn("answer");

        chatService.send(userId, conversationId, new SendMessageRequest("How do I improve my CV?"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> variables = ArgumentCaptor.forClass(Map.class);
        verify(promptTemplateService).render(eq("chat-system"), variables.capture());
        assertThat(variables.getValue().get("user_profile"))
                .contains("English", "Concise", "Java developer", "Use practical examples");
        assertThat(variables.getValue().get("career_goal")).isEqualTo("Backend role");
    }

    @Test
    void get_throwsNotFound_whenConversationBelongsToAnotherUser() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.get(userId, conversationId))
                .isInstanceOf(NotFoundException.class);
        verify(messageRepository, never()).findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    private Message message(MessageRole role, String content) {
        Message message = new Message();
        message.setRole(role);
        message.setContent(content);
        return message;
    }
}