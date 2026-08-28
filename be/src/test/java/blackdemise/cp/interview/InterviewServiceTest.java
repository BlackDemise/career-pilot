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
}
