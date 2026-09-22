package blackdemise.cp.cv;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;

import blackdemise.cp.common.exception.NotFoundException;
import blackdemise.cp.cv.dto.CvAnalysisJobResponse;
import blackdemise.cp.cv.dto.CvAnalysisResponse;
import blackdemise.cp.cv.dto.CvJdMatchRequest;
import blackdemise.cp.cv.entity.Cv;
import blackdemise.cp.cv.entity.CvAnalysisJob;
import blackdemise.cp.user.User;
import blackdemise.cp.user.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CvAnalysisJobService {

    private final CvAnalysisJobRepository jobRepository;
    private final CvRepository cvRepository;
    private final UserRepository userRepository;
    private final CvAnalysisRepository analysisRepository;
    private final CvExtractionJobService extractionJobService;
    private final CvService cvService;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @PreDestroy
    void shutdown() {
        executor.close();
    }

    @Transactional
    public CvAnalysisJobResponse startJdMatch(UUID userId, UUID cvId, CvJdMatchRequest request) {
        Cv cv = findCv(userId, cvId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        CvAnalysisJob job = new CvAnalysisJob();
        job.setUser(user);
        job.setCv(cv);
        job.setType(CvAnalysisType.JD_MATCH);
        job.setStatus(CvAnalysisJobStatus.QUEUED);
        job.setStage("QUEUED");
        job.setJobDescription(request.jobDescription().trim());
        CvAnalysisJob saved = jobRepository.save(job);
        executor.submit(() -> process(saved.getId(), userId, cvId, request));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CvAnalysisJobResponse get(UUID userId, UUID jobId) {
        return jobRepository.findByIdAndUserId(jobId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("CV analysis job not found"));
    }

    private void process(UUID jobId, UUID userId, UUID cvId, CvJdMatchRequest request) {
        CvAnalysisJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        try {
            job.setStatus(CvAnalysisJobStatus.RUNNING);
            job.setStage("EXTRACTING_CV");
            jobRepository.save(job);
            extractionJobService.ensureExtracted(userId, cvId);
            job.setStage("MATCHING_REQUIREMENTS");
            jobRepository.save(job);
            CvAnalysisResponse result = cvService.matchJobDescription(userId, cvId, request);
            job.setAnalysis(analysisRepository.findById(result.id()).orElseThrow());
            job.setStatus(CvAnalysisJobStatus.COMPLETED);
            job.setStage("COMPLETED");
            jobRepository.save(job);
        } catch (Exception ex) {
            job.setStatus(CvAnalysisJobStatus.FAILED);
            job.setStage("FAILED");
            job.setErrorMessage("Unable to complete CV analysis");
            jobRepository.save(job);
        }
    }

    private Cv findCv(UUID userId, UUID cvId) {
        return cvRepository.findById(cvId)
                .filter(cv -> cv.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("CV not found"));
    }

    private CvAnalysisJobResponse toResponse(CvAnalysisJob job) {
        return new CvAnalysisJobResponse(job.getId(), job.getCv().getId(), job.getType(), job.getStatus(),
                job.getStage(), job.getAnalysis() == null ? null : job.getAnalysis().getId(), job.getErrorMessage(),
                job.getCreatedAt(), job.getUpdatedAt());
    }
}