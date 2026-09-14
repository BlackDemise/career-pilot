package blackdemise.cp.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import blackdemise.cp.ai.AiService;
import blackdemise.cp.ai.AiStreamHandler;
import blackdemise.cp.ai.AiUsage;
import blackdemise.cp.ai.prompt.PromptTemplateService;
import blackdemise.cp.chat.dto.EditMessageRequest;
import blackdemise.cp.chat.dto.SendMessageRequest;
import blackdemise.cp.chat.entity.Conversation;
import blackdemise.cp.chat.entity.Message;
import blackdemise.cp.common.exception.BadRequestException;
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
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
        // user message save, intent save, assistant message save
        verify(messageRepository, org.mockito.Mockito.times(3)).save(any(Message.class));
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

    @Test
    void sendStream_savesUserMessageAndStreamsAssistantReplyWithUsage() throws InterruptedException {
        CountDownLatch completed = new CountDownLatch(1);
        AiUsage usage = new AiUsage(12, 8, 20);
        doAnswer(invocation -> {
            AiStreamHandler handler = invocation.getArgument(2);
            handler.onChunk("Hello");
            handler.onChunk(" there");
            handler.onComplete(usage);
            completed.countDown();
            return null;
        }).when(aiService).generateStream(any(), any(), any());

        chatService.sendStream(userId, conversationId, new SendMessageRequest("What should I practice?"));

        assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();

        ArgumentCaptor<Message> saved = ArgumentCaptor.forClass(Message.class);
        // user message save, intent save, assistant message save
        verify(messageRepository, org.mockito.Mockito.times(3)).save(saved.capture());
        Message assistantMessage = saved.getAllValues().get(2);
        assertThat(assistantMessage.getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(assistantMessage.getContent()).isEqualTo("Hello there");
        assertThat(assistantMessage.getPromptTokens()).isEqualTo(12);
        assertThat(assistantMessage.getCompletionTokens()).isEqualTo(8);
        assertThat(assistantMessage.getTotalTokens()).isEqualTo(20);
    }

    @Test
    void sendStream_classifiesOutOfScopeAndSkipsGenerationInFavorOfCannedRefusal() throws InterruptedException {
        when(aiService.classify(any(), any(), any())).thenReturn("OUT_OF_SCOPE");
        CountDownLatch completed = new CountDownLatch(1);
        doAnswer(invocation -> {
            Message saved = invocation.getArgument(0);
            if (saved.getRole() == MessageRole.ASSISTANT) {
                completed.countDown();
            }
            return saved;
        }).when(messageRepository).save(any(Message.class));

        chatService.sendStream(userId, conversationId, new SendMessageRequest("What's the weather today?"));

        assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
        verify(aiService, never()).generateStream(any(), any(), any());

        ArgumentCaptor<Message> saved = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, org.mockito.Mockito.atLeast(3)).save(saved.capture());
        Message userMessage = saved.getAllValues().get(0);
        assertThat(userMessage.getIntent()).isEqualTo(ChatIntent.OUT_OF_SCOPE);
        Message assistantMessage = saved.getAllValues().get(saved.getAllValues().size() - 1);
        assertThat(assistantMessage.getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(assistantMessage.getContent()).contains("career", "software engineering");
    }

    @Test
    void sendStream_fallsBackToGeneralCareer_whenClassificationFails() throws InterruptedException {
        when(aiService.classify(any(), any(), any())).thenThrow(new RuntimeException("classification unavailable"));
        CountDownLatch completed = new CountDownLatch(1);
        doAnswer(invocation -> {
            AiStreamHandler handler = invocation.getArgument(2);
            handler.onComplete(new AiUsage(1, 1, 2));
            completed.countDown();
            return null;
        }).when(aiService).generateStream(any(), any(), any());

        chatService.sendStream(userId, conversationId, new SendMessageRequest("How do I grow as an engineer?"));

        assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
        verify(aiService).generateStream(any(), any(), any());
    }

    @Test
    void maybeSummarize_updatesConversationSummary_onceThresholdCrossed() throws InterruptedException {
        List<Message> longHistory = new ArrayList<>();
        for (int i = 0; i < 31; i++) {
            longHistory.add(message(i % 2 == 0 ? MessageRole.USER : MessageRole.ASSISTANT, "message " + i));
        }
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)).thenReturn(longHistory);
        when(promptTemplateService.render(eq("chat-summary"), any(Map.class))).thenReturn("summary prompt");
        when(aiService.generate(eq(null), eq("summary prompt"))).thenReturn("Updated summary.");

        CountDownLatch completed = new CountDownLatch(1);
        doAnswer(invocation -> {
            AiStreamHandler handler = invocation.getArgument(2);
            handler.onComplete(new AiUsage(1, 1, 2));
            completed.countDown();
            return null;
        }).when(aiService).generateStream(any(), any(), any());

        chatService.sendStream(userId, conversationId, new SendMessageRequest("Let's keep going"));

        assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(conversation.getSummary()).isEqualTo("Updated summary.");
        assertThat(conversation.getSummarizedThroughCount()).isEqualTo(11);
    }

    @Test
    void maybeSummarize_doesNothing_whenBelowThreshold() throws InterruptedException {
        List<Message> shortHistory = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            shortHistory.add(message(MessageRole.USER, "message " + i));
        }
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)).thenReturn(shortHistory);

        CountDownLatch completed = new CountDownLatch(1);
        doAnswer(invocation -> {
            AiStreamHandler handler = invocation.getArgument(2);
            handler.onComplete(new AiUsage(1, 1, 2));
            completed.countDown();
            return null;
        }).when(aiService).generateStream(any(), any(), any());

        chatService.sendStream(userId, conversationId, new SendMessageRequest("Quick question"));

        assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(conversation.getSummary()).isNull();
        verify(aiService, never()).generate(eq(null), any(String.class));
    }

    @Test
    void regenerateStream_throwsBadRequest_whenTargetIsNotLatestAssistantMessage() {
        UUID staleMessageId = UUID.randomUUID();
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(message(MessageRole.USER, "Q", UUID.randomUUID()),
                        message(MessageRole.ASSISTANT, "A", UUID.randomUUID())));

        assertThatThrownBy(() -> chatService.regenerateStream(userId, conversationId, staleMessageId))
                .isInstanceOf(BadRequestException.class);
        verify(aiService, never()).generateStream(any(), any(), any());
    }

    @Test
    void regenerateStream_deletesLatestAssistantMessageAndStreamsReplacement() throws InterruptedException {
        UUID assistantMessageId = UUID.randomUUID();
        Message userMessage = message(MessageRole.USER, "Q", UUID.randomUUID());
        Message assistantMessage = message(MessageRole.ASSISTANT, "Old answer", assistantMessageId);
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(new ArrayList<>(List.of(userMessage, assistantMessage)));

        CountDownLatch completed = new CountDownLatch(1);
        doAnswer(invocation -> {
            AiStreamHandler handler = invocation.getArgument(2);
            handler.onComplete(new AiUsage(1, 1, 2));
            completed.countDown();
            return null;
        }).when(aiService).generateStream(any(), any(), any());

        chatService.regenerateStream(userId, conversationId, assistantMessageId);

        assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
        verify(messageRepository).delete(assistantMessage);
    }

    @Test
    void editAndStream_throwsBadRequest_whenTargetIsNotUserMessage() {
        UUID assistantMessageId = UUID.randomUUID();
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(message(MessageRole.ASSISTANT, "Answer", assistantMessageId)));

        assertThatThrownBy(() -> chatService.editAndStream(userId, conversationId, assistantMessageId,
                new EditMessageRequest("New content")))
                .isInstanceOf(BadRequestException.class);
        verify(aiService, never()).generateStream(any(), any(), any());
    }

    @Test
    void editAndStream_updatesMessageAndPrunesSubsequentHistory() throws InterruptedException {
        UUID userMessageId = UUID.randomUUID();
        Message userMessage = message(MessageRole.USER, "Old question", userMessageId);
        Message assistantMessage = message(MessageRole.ASSISTANT, "Old answer", UUID.randomUUID());
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(new ArrayList<>(List.of(userMessage, assistantMessage)));

        CountDownLatch completed = new CountDownLatch(1);
        doAnswer(invocation -> {
            AiStreamHandler handler = invocation.getArgument(2);
            handler.onComplete(new AiUsage(1, 1, 2));
            completed.countDown();
            return null;
        }).when(aiService).generateStream(any(), any(), any());

        chatService.editAndStream(userId, conversationId, userMessageId, new EditMessageRequest("New question"));

        assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(userMessage.getContent()).isEqualTo("New question");
        verify(messageRepository).deleteAll(List.of(assistantMessage));
    }

    private Message message(MessageRole role, String content) {
        Message message = new Message();
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    private Message message(MessageRole role, String content, UUID id) {
        Message message = message(role, content);
        message.setId(id);
        return message;
    }
}