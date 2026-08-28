package blackdemise.cp.interview;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import blackdemise.cp.ai.AiService;
import blackdemise.cp.ai.prompt.PromptTemplateService;
import blackdemise.cp.common.exception.BadRequestException;
import blackdemise.cp.common.exception.NotFoundException;
import blackdemise.cp.interview.dto.InterviewCatalogItem;
import blackdemise.cp.interview.dto.InterviewCatalogResponse;
import blackdemise.cp.interview.dto.InterviewTopicCatalogItem;
import blackdemise.cp.interview.dto.InterviewFinalReportResult;
import blackdemise.cp.interview.dto.InterviewQuestionResponse;
import blackdemise.cp.interview.dto.InterviewSessionResponse;
import blackdemise.cp.interview.dto.InterviewSetupRequest;
import blackdemise.cp.interview.dto.SubmitAnswerRequest;
import blackdemise.cp.interview.entity.InterviewAnswer;
import blackdemise.cp.interview.entity.InterviewCatalogRule;
import blackdemise.cp.interview.entity.InterviewEvaluation;
import blackdemise.cp.interview.entity.InterviewIntegrityEvent;
import blackdemise.cp.interview.entity.InterviewLevel;
import blackdemise.cp.interview.entity.InterviewQuestion;
import blackdemise.cp.interview.entity.InterviewRole;
import blackdemise.cp.interview.entity.InterviewSession;
import blackdemise.cp.interview.entity.InterviewTopic;
import blackdemise.cp.user.User;
import blackdemise.cp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final InterviewEvaluationRepository evaluationRepository;
    private final InterviewRoleRepository roleRepository;
    private final InterviewLevelRepository levelRepository;
    private final InterviewTopicRepository topicRepository;
    private final InterviewCatalogRuleRepository catalogRuleRepository;
    private final InterviewIntegrityEventRepository integrityEventRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final PromptTemplateService promptTemplateService;
    private final JsonMapper jsonMapper;

    @Transactional(readOnly = true)
    public InterviewCatalogResponse catalog() {
        return catalog(null, null);
    }

    @Transactional(readOnly = true)
    public InterviewCatalogResponse catalog(UUID roleId, UUID levelId) {
        List<InterviewCatalogItem> roles = roleRepository.findByActiveTrueOrderByLabelAsc().stream()
                .map(role -> new InterviewCatalogItem(role.getId(), role.getCode(), role.getLabel())).toList();
        List<InterviewCatalogItem> levels = levelRepository.findByActiveTrueOrderByLabelAsc().stream()
                .map(level -> new InterviewCatalogItem(level.getId(), level.getCode(), level.getLabel())).toList();
        var topics = topicRepository.findByActiveTrueOrderByLabelAsc().stream().map(topic -> {
            var rule = roleId == null || levelId == null ? null : catalogRuleRepository.findByRoleIdAndLevelId(roleId, levelId)
                .stream().filter(candidate -> candidate.getTopic().getId().equals(topic.getId())).findFirst().orElse(null);
            return new InterviewTopicCatalogItem(topic.getId(), topic.getCode(), topic.getLabel(), topic.getPhase(),
                rule != null && rule.isRequired(), rule != null && rule.isAllowRepeat(), rule == null ? 0 : rule.getSelectionWeight());
        }).filter(topic -> roleId == null || levelId == null || topic.selectionWeight() > 0).toList();
        return new InterviewCatalogResponse(roles, levels, topics);
    }

    @Transactional
    public InterviewSessionResponse create(UUID userId, InterviewSetupRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        InterviewRole role = roleRepository.findById(request.roleId()).filter(InterviewRole::isActive)
                .orElseThrow(() -> new BadRequestException("Unsupported interview role"));
        InterviewLevel level = levelRepository.findById(request.levelId()).filter(InterviewLevel::isActive)
                .orElseThrow(() -> new BadRequestException("Unsupported interview level"));
        List<InterviewCatalogRule> rules = catalogRuleRepository.findByRoleIdAndLevelId(role.getId(), level.getId());
        List<InterviewCatalogRule> validRules = rules.stream()
            .filter(rule -> rule.getTopic() != null && rule.getTopic().isActive())
            .toList();
        if (validRules.isEmpty() || request.minimumPrimaryQuestions() > request.maximumPrimaryQuestions()) {
            throw new BadRequestException("No valid interview plan exists for this role and level");
        }
        List<UUID> topicIds = request.topicIds() == null ? List.of() : request.topicIds();
        if (!request.randomPlan() && !validRules.stream().map(rule -> rule.getTopic().getId()).toList().containsAll(topicIds)) {
            throw new BadRequestException("One or more topics are not available for this role and level");
        }
        if (request.maximumPrimaryQuestions() > request.durationSeconds() / 120) {
            throw new BadRequestException("Question budget is too large for the selected duration");
        }

        InterviewSession session = new InterviewSession();
        session.setUser(user);
        session.setRole(role.getCode());
        session.setLevel(level.getCode());
        session.setTopic(request.randomPlan() ? "RANDOM" : topicIds.toString());
        session.setNumQuestions(request.maximumPrimaryQuestions());
        session.setDurationSeconds(request.durationSeconds());
        session.setMinimumPrimaryQuestions(request.minimumPrimaryQuestions());
        session.setMaximumPrimaryQuestions(request.maximumPrimaryQuestions());
        session.setPrimaryQuestionsAsked(0);
        session.setTotalTurns(0);
        session.setEndsAt(Instant.now().plusSeconds(request.durationSeconds()));
        session.setCurrentPhase(InterviewPhase.INTRODUCTION);
        session.setStatus(InterviewStatus.SETUP);
        session.setSelectedPlanJson(write(topicIds));
        return toResponse(sessionRepository.save(session), List.of());
    }

    @Transactional(readOnly = true)
    public List<InterviewSessionResponse> list(UUID userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(session -> toResponse(session, questionRepository.findBySessionIdOrderByOrderIndexAsc(session.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public InterviewSessionResponse get(UUID userId, UUID sessionId) {
        InterviewSession session = findSession(userId, sessionId);
        return toResponse(session, questionRepository.findBySessionIdOrderByOrderIndexAsc(sessionId));
    }

    @Transactional
    public InterviewQuestionResponse start(UUID userId, UUID sessionId) {
        InterviewSession session = findSession(userId, sessionId);
        if (session.getStatus() == InterviewStatus.COMPLETED) {
            throw new BadRequestException("Interview is already completed");
        }
        if (session.getEndsAt().isBefore(Instant.now())) {
            complete(session);
            throw new BadRequestException("Interview duration has expired");
        }
        InterviewQuestion question = new InterviewQuestion();
        question.setSession(session);
        question.setOrderIndex(session.getPrimaryQuestionsAsked() + 1);
        question.setPhase(session.getCurrentPhase());
        question.setTopic(session.getTopic());
        question.setDifficulty(null);
        String prompt = promptTemplateService.render("interview-question", Map.of(
                "role", session.getRole(), "level", session.getLevel(), "topic", session.getTopic(),
                "difficulty", "derived from the role, level, phase, and conversation"));
        question.setContent(aiService.generate(null, prompt).trim());
        question = questionRepository.save(question);
        session.setStatus(statusFor(session.getCurrentPhase()));
        session.setCurrentQuestionId(question.getId());
        session.setTotalTurns(session.getTotalTurns() + 1);
        sessionRepository.save(session);
        return new InterviewQuestionResponse(question.getId(), question.getOrderIndex(), question.getContent(),
                question.getTopic(), question.getDifficulty(), null);
    }

    @Transactional
    public InterviewSessionResponse submitAnswer(UUID userId, UUID sessionId, UUID questionId,
            SubmitAnswerRequest request) {
        InterviewSession session = findSession(userId, sessionId);
        if (!questionId.equals(session.getCurrentQuestionId())) {
            throw new BadRequestException("Only the current interview question can be answered");
        }
        if (session.getEndsAt().isBefore(Instant.now())) {
            complete(session);
            throw new BadRequestException("Interview duration has expired");
        }
        InterviewQuestion question = questionRepository.findById(questionId)
                .filter(candidate -> candidate.getSession().getId().equals(sessionId))
                .orElseThrow(() -> new NotFoundException("Interview question not found"));
        if (answerRepository.findByQuestionId(questionId).isPresent()) {
            throw new BadRequestException("An answer has already been submitted for this question");
        }
        InterviewAnswer answer = new InterviewAnswer();
        answer.setQuestion(question);
        answer.setContent(request.content().trim());
        answer.setSubmittedAt(Instant.now());
        answer.setTimedOut(false);
        answerRepository.save(answer);
        session.setPrimaryQuestionsAsked(session.getPrimaryQuestionsAsked() + 1);
        session.setCurrentQuestionId(null);
        session.setCurrentPhase(nextPhase(session.getCurrentPhase(), session.getPrimaryQuestionsAsked(), session.getMaximumPrimaryQuestions()));
        if (session.getPrimaryQuestionsAsked() >= session.getMaximumPrimaryQuestions()) {
            complete(session);
        } else {
            session.setStatus(statusFor(session.getCurrentPhase()));
            sessionRepository.save(session);
        }
        return get(userId, sessionId);
    }

    @Transactional
    public InterviewSessionResponse timeout(UUID userId, UUID sessionId, UUID questionId) {
        InterviewSession session = findSession(userId, sessionId);
        if (!questionId.equals(session.getCurrentQuestionId())) {
            throw new BadRequestException("Only the current interview question can time out");
        }
        InterviewQuestion question = questionRepository.findById(questionId)
                .filter(candidate -> candidate.getSession().getId().equals(sessionId))
                .orElseThrow(() -> new NotFoundException("Interview question not found"));
        if (answerRepository.findByQuestionId(questionId).isPresent()) {
            throw new BadRequestException("The current interview question is already closed");
        }
        InterviewAnswer answer = new InterviewAnswer();
        answer.setQuestion(question);
        answer.setContent("");
        answer.setSubmittedAt(Instant.now());
        answer.setTimedOut(true);
        answerRepository.save(answer);
        session.setPrimaryQuestionsAsked(session.getPrimaryQuestionsAsked() + 1);
        session.setCurrentQuestionId(null);
        if (session.getPrimaryQuestionsAsked() >= session.getMaximumPrimaryQuestions()) {
            complete(session);
        } else {
            session.setCurrentPhase(nextPhase(session.getCurrentPhase(), session.getPrimaryQuestionsAsked(), session.getMaximumPrimaryQuestions()));
            session.setStatus(statusFor(session.getCurrentPhase()));
            sessionRepository.save(session);
        }
        return get(userId, sessionId);
    }

    @Transactional
    public void recordIntegrityEvent(UUID userId, UUID sessionId, String eventType, String metadata) {
        InterviewSession session = findSession(userId, sessionId);
        InterviewIntegrityEvent event = new InterviewIntegrityEvent();
        event.setSession(session);
        event.setEventType(eventType);
        event.setOccurredAt(Instant.now());
        event.setMetadata(metadata);
        integrityEventRepository.save(event);
    }

    @Transactional
    public InterviewFinalReportResult finalReport(UUID userId, UUID sessionId) {
        InterviewSession session = findSession(userId, sessionId);
        if (session.getStatus() != InterviewStatus.COMPLETED) {
            throw new BadRequestException("Interview must be completed before requesting its report");
        }
        String summary = questionRepository.findBySessionIdOrderByOrderIndexAsc(sessionId).stream()
                .map(question -> "Question: " + question.getContent() + "\nAnswer: "
                        + answerRepository.findByQuestionId(question.getId()).map(InterviewAnswer::getContent).orElse("No answer"))
                .reduce("", (left, right) -> left + right + "\n\n");
        String prompt = promptTemplateService.render("interview-final-report", Map.of("interview_summary", summary));
        InterviewFinalReportResult result = parse(aiService.generate(null, prompt), InterviewFinalReportResult.class);
        if (result.overallScore() == null || result.overallScore() < 0 || result.overallScore() > 10) {
            throw new BadRequestException("Interview report score must be between 0 and 10");
        }
        return result;
    }

    private InterviewSession findSession(UUID userId, UUID sessionId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Interview session not found"));
    }

    private void complete(InterviewSession session) {
        session.setCurrentPhase(InterviewPhase.CLOSING);
        session.setCurrentQuestionId(null);
        session.setStatus(InterviewStatus.COMPLETED);
        sessionRepository.save(session);
    }

    private InterviewStatus statusFor(InterviewPhase phase) {
        return switch (phase) {
            case INTRODUCTION -> InterviewStatus.INTRODUCTION;
            case WARM_UP -> InterviewStatus.WARM_UP;
            case TECHNICAL -> InterviewStatus.TECHNICAL;
            case SITUATIONAL -> InterviewStatus.SITUATIONAL;
            case CLOSING -> InterviewStatus.CLOSING;
        };
    }

    private InterviewPhase nextPhase(InterviewPhase phase, int answered, int maximum) {
        if (answered >= maximum) return InterviewPhase.CLOSING;
        return switch (phase) {
            case INTRODUCTION -> InterviewPhase.WARM_UP;
            case WARM_UP -> InterviewPhase.TECHNICAL;
            case TECHNICAL -> answered * 2 >= maximum ? InterviewPhase.SITUATIONAL : InterviewPhase.TECHNICAL;
            case SITUATIONAL, CLOSING -> InterviewPhase.CLOSING;
        };
    }

    private InterviewSessionResponse toResponse(InterviewSession session, List<InterviewQuestion> questions) {
        return new InterviewSessionResponse(session.getId(), session.getRole(), session.getLevel(), session.getTopic(),
                null, session.getNumQuestions(), session.getDurationSeconds(), session.getMinimumPrimaryQuestions(),
                session.getMaximumPrimaryQuestions(), session.getPrimaryQuestionsAsked(), session.getTotalTurns(),
                session.getEndsAt(), session.getCurrentPhase(), session.getStatus(), session.getCreatedAt(),
                session.getUpdatedAt(), questions.stream().map(question -> new InterviewQuestionResponse(question.getId(),
                        question.getOrderIndex(), question.getContent(), question.getTopic(), question.getDifficulty(),
                        answerRepository.findByQuestionId(question.getId()).map(InterviewAnswer::getContent).orElse(null))).toList());
    }

    private <T> T parse(String raw, Class<T> type) {
        try { return jsonMapper.readValue(raw == null ? "" : raw.trim(), type); }
        catch (RuntimeException ex) { throw new BadRequestException("AI returned an invalid interview result"); }
    }

    private String write(Object value) {
        try { return jsonMapper.writeValueAsString(value); }
        catch (JacksonException ex) { throw new IllegalStateException("Failed to store interview plan", ex); }
    }
}
