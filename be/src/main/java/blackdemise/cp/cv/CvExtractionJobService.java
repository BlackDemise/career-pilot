package blackdemise.cp.cv;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;

import blackdemise.cp.ai.AiService;
import blackdemise.cp.ai.prompt.PromptTemplateService;
import blackdemise.cp.common.exception.BadRequestException;
import blackdemise.cp.common.exception.NotFoundException;
import blackdemise.cp.cv.dto.CvExtractionJobResponse;
import blackdemise.cp.cv.dto.CvStructuredExtraction;
import blackdemise.cp.cv.entity.Cv;
import blackdemise.cp.cv.entity.CvExtractionJob;
import blackdemise.cp.user.User;
import blackdemise.cp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class CvExtractionJobService {

    private static final int EXTRACTION_VERSION = 1;

    private final CvRepository cvRepository;
    private final CvExtractionJobRepository jobRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final PromptTemplateService promptTemplateService;
    private final JsonMapper jsonMapper;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @PreDestroy
    void shutdown() {
        executor.close();
    }

    @Transactional
    public CvExtractionJobResponse start(UUID userId, UUID cvId) {
        Cv cv = findCv(userId, cvId);
        if (cv.getExtractionStatus() == CvExtractionStatus.COMPLETED) {
            throw new BadRequestException("CV structured extraction is already completed");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        CvExtractionJob job = new CvExtractionJob();
        job.setUser(user);
        job.setCv(cv);
        job.setStatus(CvExtractionJobStatus.QUEUED);
        job.setStage("QUEUED");
        CvExtractionJob saved = jobRepository.save(job);
        executor.submit(() -> process(saved.getId(), cv.getId()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CvExtractionJobResponse get(UUID userId, UUID jobId) {
        return jobRepository.findByIdAndUserId(jobId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("CV extraction job not found"));
    }

    @Transactional
    public void ensureExtracted(UUID userId, UUID cvId) {
        Cv cv = findCv(userId, cvId);
        if (cv.getExtractionStatus() == CvExtractionStatus.COMPLETED) {
            return;
        }
        extractCv(cv);
    }

    private void process(UUID jobId, UUID cvId) {
        CvExtractionJob job = jobRepository.findById(jobId).orElse(null);
        Cv cv = cvRepository.findById(cvId).orElse(null);
        if (job == null || cv == null) {
            return;
        }
        try {
            job.setStatus(CvExtractionJobStatus.RUNNING);
            job.setStage("EXTRACTING_CV");
            jobRepository.save(job);
                extractCv(cv);

            job.setStatus(CvExtractionJobStatus.COMPLETED);
            job.setStage("COMPLETED");
            jobRepository.save(job);
        } catch (Exception ex) {
            cv.setExtractionStatus(CvExtractionStatus.FAILED);
            cv.setExtractionError("Unable to extract structured CV data");
            cvRepository.save(cv);
            job.setStatus(CvExtractionJobStatus.FAILED);
            job.setStage("FAILED");
            job.setErrorMessage("Unable to extract structured CV data");
            jobRepository.save(job);
        }
    }

    private void extractCv(Cv cv) {
        cv.setExtractionStatus(CvExtractionStatus.PROCESSING);
        cv.setExtractionError(null);
        cvRepository.save(cv);

        String prompt = promptTemplateService.render("cv-structured-extraction",
                Map.of("cv", cv.getExtractedText()));
        CvStructuredExtraction result = parse(aiService.generate(null, prompt));
        cv.setStructuredExtractionJson(write(result));
        cv.setStructuredExtractionVersion(EXTRACTION_VERSION);
        cv.setExtractionStatus(CvExtractionStatus.COMPLETED);
        cvRepository.save(cv);
    }

    private Cv findCv(UUID userId, UUID cvId) {
        return cvRepository.findById(cvId)
                .filter(cv -> cv.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("CV not found"));
    }

    private CvStructuredExtraction parse(String raw) {
        try {
            return jsonMapper.readValue(raw.trim(), CvStructuredExtraction.class);
        } catch (JacksonException ex) {
            throw new BadRequestException("AI returned an invalid structured CV result");
        }
    }

    private String write(CvStructuredExtraction result) {
        try {
            return jsonMapper.writeValueAsString(result);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to store structured CV result", ex);
        }
    }

    private CvExtractionJobResponse toResponse(CvExtractionJob job) {
        return new CvExtractionJobResponse(job.getId(), job.getCv().getId(), job.getStatus(), job.getStage(),
                job.getErrorMessage(), job.getCreatedAt(), job.getUpdatedAt());
    }
}