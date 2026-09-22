package blackdemise.cp.cv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import blackdemise.cp.common.exception.NotFoundException;
import blackdemise.cp.cv.dto.CvJdMatchRequest;
import blackdemise.cp.cv.entity.Cv;
import blackdemise.cp.cv.entity.CvAnalysisJob;
import blackdemise.cp.user.User;
import blackdemise.cp.user.UserRepository;

class CvAnalysisJobServiceTest {

    private final CvAnalysisJobRepository jobRepository = mock(CvAnalysisJobRepository.class);
    private final CvRepository cvRepository = mock(CvRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CvAnalysisRepository analysisRepository = mock(CvAnalysisRepository.class);
    private final CvExtractionJobService extractionJobService = mock(CvExtractionJobService.class);
    private final CvService cvService = mock(CvService.class);
    private final CvAnalysisJobService service = new CvAnalysisJobService(jobRepository, cvRepository,
            userRepository, analysisRepository, extractionJobService, cvService);

    private final UUID userId = UUID.randomUUID();
    private final UUID cvId = UUID.randomUUID();

    @AfterEach
    void closeExecutor() {
        service.shutdown();
    }

    @Test
    void startQueuesJdMatchForOwnedCv() {
        User user = new User();
        user.setId(userId);
        Cv cv = new Cv();
        cv.setId(cvId);
        cv.setUser(user);
        when(cvRepository.findById(cvId)).thenReturn(Optional.of(cv));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jobRepository.save(any(CvAnalysisJob.class))).thenAnswer(invocation -> {
            CvAnalysisJob job = invocation.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });

        var result = service.startJdMatch(userId, cvId, new CvJdMatchRequest("Java role"));

        assertThat(result.cvId()).isEqualTo(cvId);
        assertThat(result.type()).isEqualTo(CvAnalysisType.JD_MATCH);
        assertThat(result.status()).isEqualTo(CvAnalysisJobStatus.QUEUED);
    }

    @Test
    void getRejectsJobOwnedByAnotherUser() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findByIdAndUserId(jobId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(userId, jobId)).isInstanceOf(NotFoundException.class);
    }
}