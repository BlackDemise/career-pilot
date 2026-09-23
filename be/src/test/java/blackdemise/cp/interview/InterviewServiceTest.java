package blackdemise.cp.interview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import blackdemise.cp.ai.AiService;
import blackdemise.cp.ai.prompt.PromptTemplateService;
import blackdemise.cp.common.exception.BadRequestException;
import blackdemise.cp.interview.dto.InterviewSetupRequest;
import blackdemise.cp.interview.entity.InterviewCatalogRule;
import blackdemise.cp.interview.entity.InterviewLevel;
import blackdemise.cp.interview.entity.InterviewRole;
import blackdemise.cp.interview.entity.InterviewTopic;
import blackdemise.cp.user.UserRepository;
import tools.jackson.databind.json.JsonMapper;

class InterviewServiceTest {
    private final InterviewSessionRepository sessions = mock(InterviewSessionRepository.class);
    private final InterviewQuestionRepository questions = mock(InterviewQuestionRepository.class);
    private final InterviewAnswerRepository answers = mock(InterviewAnswerRepository.class);
    private final InterviewEvaluationRepository evaluations = mock(InterviewEvaluationRepository.class);
    private final InterviewRoleRepository roles = mock(InterviewRoleRepository.class);
    private final InterviewLevelRepository levels = mock(InterviewLevelRepository.class);
    private final InterviewTopicRepository topics = mock(InterviewTopicRepository.class);
    private final InterviewCatalogRuleRepository rules = mock(InterviewCatalogRuleRepository.class);
    private final InterviewIntegrityEventRepository integrity = mock(InterviewIntegrityEventRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AiService ai = mock(AiService.class);
    private final PromptTemplateService prompts = mock(PromptTemplateService.class);
    private final InterviewService service = new InterviewService(sessions, questions, answers, evaluations, roles,
            levels, topics, rules, integrity, users, ai, prompts, JsonMapper.builder().build());

    @Test
    void setupRejectsQuestionBudgetThatCannotFitDuration() {
        UUID roleId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        InterviewRole role = new InterviewRole();
        role.setId(roleId);
        role.setCode("BACKEND_DEVELOPER");
        role.setActive(true);
        InterviewLevel level = new InterviewLevel();
        level.setId(levelId);
        level.setCode("SENIOR");
        level.setActive(true);
        when(users.findById(any())).thenReturn(Optional.of(new blackdemise.cp.user.User()));
        when(roles.findById(roleId)).thenReturn(Optional.of(role));
        when(levels.findById(levelId)).thenReturn(Optional.of(level));
        when(rules.findByRoleIdAndLevelId(roleId, levelId)).thenReturn(List.of(new InterviewCatalogRule()));

        assertThatThrownBy(() -> service.create(UUID.randomUUID(),
                new InterviewSetupRequest(roleId, levelId, List.of(UUID.randomUUID()), 60, 1, 30, false)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("valid interview plan");
    }

    @Test
    void catalogReturnsDatabaseBackedRolesLevelsAndTopics() {
        InterviewRole role = new InterviewRole();
        role.setId(UUID.randomUUID());
        role.setCode("WEB_DEVELOPER");
        role.setLabel("Web Developer");
        InterviewLevel level = new InterviewLevel();
        level.setId(UUID.randomUUID());
        level.setCode("JUNIOR");
        level.setLabel("Junior");
        InterviewTopic topic = new InterviewTopic();
        topic.setId(UUID.randomUUID());
        topic.setCode("HTTP");
        topic.setLabel("HTTP basics");
        topic.setPhase("TECHNICAL");
        when(roles.findByActiveTrueOrderByLabelAsc()).thenReturn(List.of(role));
        when(levels.findByActiveTrueOrderByLabelAsc()).thenReturn(List.of(level));
        when(topics.findByActiveTrueOrderByLabelAsc()).thenReturn(List.of(topic));

        var catalog = service.catalog();

        assertThat(catalog.roles()).extracting("code").containsExactly("WEB_DEVELOPER");
        assertThat(catalog.levels()).extracting("code").containsExactly("JUNIOR");
        assertThat(catalog.topics()).extracting("code").containsExactly("HTTP");
    }

    @Test
    void createRandomPlanStartsInPreparingStateAndPersistsValidSelectedTopics() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        UUID requiredTopicId = UUID.randomUUID();
        UUID optionalTopicId = UUID.randomUUID();

        InterviewRole role = new InterviewRole();
        role.setId(roleId);
        role.setCode("BACKEND_DEVELOPER");
        role.setLabel("Backend Developer");
        role.setActive(true);

        InterviewLevel level = new InterviewLevel();
        level.setId(levelId);
        level.setCode("JUNIOR");
        level.setLabel("Junior");
        level.setActive(true);

        InterviewTopic requiredTopic = new InterviewTopic();
        requiredTopic.setId(requiredTopicId);
        requiredTopic.setCode("APIS");
        requiredTopic.setLabel("APIs");
        requiredTopic.setPhase("TECHNICAL");
        requiredTopic.setActive(true);

        InterviewTopic optionalTopic = new InterviewTopic();
        optionalTopic.setId(optionalTopicId);
        optionalTopic.setCode("DATABASES");
        optionalTopic.setLabel("Databases");
        optionalTopic.setPhase("TECHNICAL");
        optionalTopic.setActive(true);

        InterviewCatalogRule requiredRule = new InterviewCatalogRule();
        requiredRule.setRole(role);
        requiredRule.setLevel(level);
        requiredRule.setTopic(requiredTopic);
        requiredRule.setRequired(true);
        requiredRule.setAllowRepeat(false);
        requiredRule.setSelectionWeight(10);

        InterviewCatalogRule optionalRule = new InterviewCatalogRule();
        optionalRule.setRole(role);
        optionalRule.setLevel(level);
        optionalRule.setTopic(optionalTopic);
        optionalRule.setRequired(false);
        optionalRule.setAllowRepeat(false);
        optionalRule.setSelectionWeight(5);

        when(users.findById(userId)).thenReturn(Optional.of(new blackdemise.cp.user.User()));
        when(roles.findById(roleId)).thenReturn(Optional.of(role));
        when(levels.findById(levelId)).thenReturn(Optional.of(level));
        when(rules.findByRoleIdAndLevelId(roleId, levelId)).thenReturn(List.of(requiredRule, optionalRule));
        when(sessions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(userId, new InterviewSetupRequest(roleId, levelId, null, 600, 1, 3, true));

        assertThat(response.status()).isEqualTo(InterviewStatus.PREPARING);
        assertThat(response.currentPhase()).isEqualTo(InterviewPhase.INTRODUCTION);
    }

    @Test
    void startingPreparedSessionCreatesQuestionAndMarksInterviewLive() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        blackdemise.cp.interview.entity.InterviewSession session = new blackdemise.cp.interview.entity.InterviewSession();
        session.setId(sessionId);
        session.setUser(new blackdemise.cp.user.User());
        session.setRole("BACKEND_DEVELOPER");
        session.setLevel("JUNIOR");
        session.setTopic("RANDOM");
        session.setDurationSeconds(600);
        session.setMinimumPrimaryQuestions(1);
        session.setMaximumPrimaryQuestions(3);
        session.setCurrentPhase(InterviewPhase.INTRODUCTION);
        session.setStatus(InterviewStatus.PREPARING);
        session.setEndsAt(java.time.Instant.now().plusSeconds(600));

        when(sessions.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));
        when(prompts.render(any(), any())).thenReturn("question prompt");
        when(ai.generate(any(), any())).thenReturn("What is your approach to APIs?");
        when(questions.save(any())).thenAnswer(invocation -> {
            blackdemise.cp.interview.entity.InterviewQuestion question = invocation.getArgument(0);
            question.setId(questionId);
            return question;
        });
        when(sessions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.start(userId, sessionId);

        assertThat(result.content()).contains("What is your approach to APIs?");
        assertThat(session.getStatus()).isEqualTo(InterviewStatus.INTRODUCTION);
        assertThat(session.getCurrentQuestionId()).isEqualTo(questionId);
        assertThat(session.getTotalTurns()).isEqualTo(1);
    }

    @Test
    void timeoutProgressesSessionAndCountsUnansweredTurn() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        blackdemise.cp.interview.entity.InterviewSession session = new blackdemise.cp.interview.entity.InterviewSession();
        session.setId(sessionId);
        session.setUser(new blackdemise.cp.user.User());
        session.setRole("BACKEND_DEVELOPER");
        session.setLevel("JUNIOR");
        session.setTopic("RANDOM");
        session.setDurationSeconds(600);
        session.setMinimumPrimaryQuestions(1);
        session.setMaximumPrimaryQuestions(3);
        session.setCurrentPhase(InterviewPhase.INTRODUCTION);
        session.setStatus(InterviewStatus.INTRODUCTION);
        session.setCurrentQuestionId(questionId);
        session.setEndsAt(java.time.Instant.now().plusSeconds(600));

        blackdemise.cp.interview.entity.InterviewQuestion question = new blackdemise.cp.interview.entity.InterviewQuestion();
        question.setId(questionId);
        question.setSession(session);
        question.setPhase(InterviewPhase.INTRODUCTION);

        when(sessions.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));
        when(questions.findById(questionId)).thenReturn(Optional.of(question));
        when(answers.findByQuestionId(questionId)).thenReturn(Optional.empty());
        when(answers.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(questions.findBySessionIdOrderByOrderIndexAsc(sessionId)).thenReturn(List.of());

        var result = service.timeout(userId, sessionId, questionId);

        assertThat(session.getPrimaryQuestionsAsked()).isEqualTo(1);
        assertThat(session.getCurrentPhase()).isEqualTo(InterviewPhase.WARM_UP);
        assertThat(session.getStatus()).isEqualTo(InterviewStatus.WARM_UP);
        assertThat(result.status()).isEqualTo(InterviewStatus.WARM_UP);
    }

    @Test
    void recordIntegrityEventRejectsBlankEventType() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        blackdemise.cp.interview.entity.InterviewSession session = new blackdemise.cp.interview.entity.InterviewSession();
        session.setId(sessionId);
        session.setUser(new blackdemise.cp.user.User());
        session.setStatus(InterviewStatus.INTRODUCTION);

        when(sessions.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.recordIntegrityEvent(userId, sessionId, "  ", "{}"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Integrity event type is required");
    }

    @Test
    void finalReportRejectsIncompleteInterview() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        blackdemise.cp.interview.entity.InterviewSession session = new blackdemise.cp.interview.entity.InterviewSession();
        session.setId(sessionId);
        session.setUser(new blackdemise.cp.user.User());
        session.setStatus(InterviewStatus.PREPARING);

        when(sessions.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.finalReport(userId, sessionId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Interview must be completed");
    }
}
