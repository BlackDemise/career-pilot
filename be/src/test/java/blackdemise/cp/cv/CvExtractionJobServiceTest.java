package blackdemise.cp.cv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import blackdemise.cp.ai.AiService;
import blackdemise.cp.ai.prompt.PromptTemplateService;
import blackdemise.cp.common.exception.NotFoundException;
import blackdemise.cp.cv.entity.Cv;
import blackdemise.cp.cv.entity.CvExtractionJob;
import blackdemise.cp.user.User;
import blackdemise.cp.user.UserRepository;
import tools.jackson.databind.json.JsonMapper;

class CvExtractionJobServiceTest {

    private final CvRepository cvRepository = mock(CvRepository.class);
    private final CvExtractionJobRepository jobRepository = mock(CvExtractionJobRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AiService aiService = mock(AiService.class);
    private final PromptTemplateService promptTemplateService = mock(PromptTemplateService.class);
    private final CvExtractionJobService service = new CvExtractionJobService(cvRepository, jobRepository,
            userRepository, aiService, promptTemplateService, JsonMapper.builder().build());

    private final UUID userId = UUID.randomUUID();
    private final UUID cvId = UUID.randomUUID();

    @AfterEach
    void closeExecutor() {
        service.shutdown();
    }

    @Test
    void startQueuesExtractionForOwnedCv() {
        User user = new User();
        user.setId(userId);
        Cv cv = new Cv();
        cv.setId(cvId);
        cv.setUser(user);
        when(cvRepository.findById(cvId)).thenReturn(Optional.of(cv));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jobRepository.save(any(CvExtractionJob.class))).thenAnswer(invocation -> {
            CvExtractionJob job = invocation.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });

        var result = service.start(userId, cvId);

        assertThat(result.cvId()).isEqualTo(cvId);
        assertThat(result.status()).isEqualTo(CvExtractionJobStatus.QUEUED);
        assertThat(result.stage()).isEqualTo("QUEUED");
        verify(jobRepository).save(any(CvExtractionJob.class));
    }

    @Test
    void getRejectsJobOwnedByAnotherUser() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findByIdAndUserId(jobId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(userId, jobId)).isInstanceOf(NotFoundException.class);
    }
}