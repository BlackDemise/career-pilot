package blackdemise.cp.cv;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import blackdemise.cp.ai.AiService;
import blackdemise.cp.ai.prompt.PromptTemplateService;
import blackdemise.cp.common.exception.BadRequestException;
import blackdemise.cp.common.exception.NotFoundException;
import blackdemise.cp.cv.dto.CvAnalysisResponse;
import blackdemise.cp.cv.dto.CvJdMatchRequest;
import blackdemise.cp.cv.dto.CvJdMatchResult;
import blackdemise.cp.cv.dto.CvResponse;
import blackdemise.cp.cv.dto.CvReviewResult;
import blackdemise.cp.cv.entity.Cv;
import blackdemise.cp.cv.entity.CvAnalysis;
import blackdemise.cp.user.User;
import blackdemise.cp.user.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CvService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final long DEFAULT_MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final CvRepository cvRepository;
    private final CvAnalysisRepository cvAnalysisRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final PromptTemplateService promptTemplateService;
    private final JsonMapper jsonMapper;

    @Value("${app.cv.max-file-size-bytes:5242880}")
    private long maxFileSizeBytes = DEFAULT_MAX_FILE_SIZE;

    @Transactional
    public CvResponse upload(UUID userId, MultipartFile file) {
        validatePdf(file);
        String extractedText = extractText(file);
        if (extractedText.isBlank()) {
            throw new BadRequestException("The PDF does not contain extractable text");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Cv cv = new Cv();
        cv.setUser(user);
        cv.setFileName(safeFileName(file.getOriginalFilename()));
        cv.setExtractedText(extractedText);
        return toCvResponse(cvRepository.save(cv));
    }

    @Transactional
    public CvAnalysisResponse review(UUID userId, UUID cvId) {
        Cv cv = findCv(userId, cvId);
        String prompt = promptTemplateService.render("cv-review", Map.of("cv", cv.getExtractedText()));
        CvReviewResult result = parse(aiService.generate(null, prompt), CvReviewResult.class);
        return saveAnalysis(cv, CvAnalysisType.REVIEW, null, result);
    }

    @Transactional
    public CvAnalysisResponse matchJobDescription(UUID userId, UUID cvId, CvJdMatchRequest request) {
        Cv cv = findCv(userId, cvId);
        String prompt = promptTemplateService.render("cv-jd-analysis", Map.of(
                "cv", cv.getExtractedText(), "jd", request.jobDescription().trim()));
        CvJdMatchResult result = parse(aiService.generate(null, prompt), CvJdMatchResult.class);
        return saveAnalysis(cv, CvAnalysisType.JD_MATCH, request.jobDescription().trim(), result);
    }

    @Transactional(readOnly = true)
    public List<CvAnalysisResponse> listAnalyses(UUID userId, UUID cvId) {
        Cv cv = findCv(userId, cvId);
        return cvAnalysisRepository.findByCvId(cv.getId()).stream()
                .map(analysis -> toAnalysisResponse(analysis, parseResult(analysis)))
                .toList();
    }

    private Cv findCv(UUID userId, UUID cvId) {
        return cvRepository.findById(cvId)
                .filter(cv -> cv.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("CV not found"));
    }

    private CvAnalysisResponse saveAnalysis(Cv cv, CvAnalysisType type, String jobDescription, Object result) {
        CvAnalysis analysis = new CvAnalysis();
        analysis.setCv(cv);
        analysis.setType(type);
        analysis.setJobDescription(jobDescription);
        analysis.setResultJson(write(result));
        return toAnalysisResponse(cvAnalysisRepository.save(analysis), result);
    }

    private Object parseResult(CvAnalysis analysis) {
        return analysis.getType() == CvAnalysisType.REVIEW
                ? parse(analysis.getResultJson(), CvReviewResult.class)
                : parse(analysis.getResultJson(), CvJdMatchResult.class);
    }

    private <T> T parse(String raw, Class<T> type) {
        String json = raw.trim();
        if (json.startsWith("```") && json.endsWith("```")) {
            json = json.substring(json.indexOf('\n') + 1, json.length() - 3).trim();
        }
        try {
            return jsonMapper.readValue(json, type);
        } catch (JacksonException ex) {
            throw new BadRequestException("AI returned an invalid CV analysis result");
        }
    }

    private String write(Object result) {
        try {
            return jsonMapper.writeValueAsString(result);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to store CV analysis result", ex);
        }
    }

    private CvResponse toCvResponse(Cv cv) {
        return new CvResponse(cv.getId(), cv.getFileName(), cv.getExtractedText(), cv.getCreatedAt());
    }

    private CvAnalysisResponse toAnalysisResponse(CvAnalysis analysis, Object result) {
        return new CvAnalysisResponse(analysis.getId(), analysis.getCv().getId(), analysis.getType(), result,
                analysis.getJobDescription(), analysis.getCreatedAt());
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A PDF CV file is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("CV file must not exceed " + maxFileSizeBytes + " bytes");
        }
        String fileName = file.getOriginalFilename();
        if (!PDF_CONTENT_TYPE.equalsIgnoreCase(file.getContentType())
                && (fileName == null || !fileName.toLowerCase().endsWith(".pdf"))) {
            throw new BadRequestException("Only PDF CV files are supported");
        }
    }

    private String extractText(MultipartFile file) {
        try (var document = Loader.loadPDF(file.getBytes())) {
            return new PDFTextStripper().getText(document).trim();
        } catch (IOException | RuntimeException ex) {
            throw new BadRequestException("Unable to read the PDF CV");
        }
    }

    private String safeFileName(String originalFilename) {
        String fileName = originalFilename == null ? "cv.pdf" : originalFilename;
        fileName = fileName.replace('\\', '/');
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1).trim();
        return fileName.isBlank() ? "cv.pdf" : fileName;
    }
}
