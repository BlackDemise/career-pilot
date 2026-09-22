package blackdemise.cp.cv;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
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
import blackdemise.cp.cv.dto.CvRequirementMatchesResponse;
import blackdemise.cp.cv.dto.CvRequirementsResponse;
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
        DocumentFormat format = validateFile(file);
        String extractedText = extractText(file, format);
        if (extractedText.isBlank()) {
            throw new BadRequestException("The CV does not contain extractable text");
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
        String jobDescription = request.jobDescription().trim();
        String requirementsPrompt = promptTemplateService.render("cv-jd-requirements",
            Map.of("jd", jobDescription));
        CvRequirementsResponse requirementsResponse = parse(
            aiService.generate(null, requirementsPrompt), CvRequirementsResponse.class);
        String requirementsJson = write(requirementsResponse);
        String matchingPrompt = promptTemplateService.render("cv-jd-requirement-matching", Map.of(
            "cv", cv.getExtractedText(), "requirements", requirementsJson));
        CvRequirementMatchesResponse matchesResponse = parse(
            aiService.generate(null, matchingPrompt), CvRequirementMatchesResponse.class);
        List<blackdemise.cp.cv.dto.CvRequirementMatch> verifiedMatches = CvEvidenceVerifier.verify(
            cv.getExtractedText(), matchesResponse.requirementMatches());
        CvMatchScoreCalculator.Score score = CvMatchScoreCalculator.calculate(
            requirementsResponse.requirements(), verifiedMatches);
        CvJdMatchResult result = new CvJdMatchResult(score.overallScore(), requirementsResponse.requirements(),
            verifiedMatches, score.sectionScores(), matchesResponse.matchedSkills(),
            matchesResponse.missingSkills(), matchesResponse.experienceGaps(), matchesResponse.recommendations());
        return saveAnalysis(cv, CvAnalysisType.JD_MATCH, jobDescription, result);
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

    private DocumentFormat validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A PDF or DOCX CV file is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("CV file must not exceed " + maxFileSizeBytes + " bytes");
        }
        String fileName = safeFileName(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        DocumentFormat format;
        if (fileName.endsWith(".pdf")) {
            format = DocumentFormat.PDF;
        } else if (fileName.endsWith(".docx")) {
            format = DocumentFormat.DOCX;
        } else {
            throw new BadRequestException("Only PDF and DOCX CV files are supported");
        }
        try {
            byte[] content = file.getBytes();
            if (!format.matches(content)) {
                throw new BadRequestException("The CV content does not match its file extension");
            }
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read the CV file");
        }
        return format;
    }

    private String extractText(MultipartFile file, DocumentFormat format) {
        try {
            byte[] content = file.getBytes();
            return switch (format) {
                case PDF -> extractPdfText(content);
                case DOCX -> extractDocxText(content);
            };
        } catch (IOException | RuntimeException ex) {
            throw new BadRequestException("Unable to read the " + format.name() + " CV");
        }
    }

    private String extractPdfText(byte[] content) throws IOException {
        try (var document = Loader.loadPDF(content)) {
            return new PDFTextStripper().getText(document).trim();
        }
    }

    private String extractDocxText(byte[] content) throws IOException {
        StringBuilder text = new StringBuilder();
        try (var document = new XWPFDocument(new ByteArrayInputStream(content))) {
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    appendLine(text, paragraph.getText());
                } else if (element instanceof XWPFTable table) {
                    for (XWPFTableRow row : table.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            appendLine(text, cell.getText());
                        }
                    }
                }
            }
        }
        return text.toString().trim();
    }

    private void appendLine(StringBuilder text, String value) {
        if (value != null && !value.isBlank()) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(value.trim());
        }
    }

    private enum DocumentFormat {
        PDF {
            @Override
            boolean matches(byte[] content) {
                return content.length >= 5 && content[0] == '%' && content[1] == 'P'
                        && content[2] == 'D' && content[3] == 'F' && content[4] == '-';
            }
        },
        DOCX {
            @Override
            boolean matches(byte[] content) {
                if (content.length < 4 || content[0] != 'P' || content[1] != 'K') {
                    return false;
                }
                boolean contentTypes = false;
                boolean document = false;
                try (var zip = new ZipInputStream(new ByteArrayInputStream(content))) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        contentTypes |= "[Content_Types].xml".equals(entry.getName());
                        document |= "word/document.xml".equals(entry.getName());
                    }
                    return contentTypes && document;
                } catch (IOException ex) {
                    return false;
                }
            }
        };

        abstract boolean matches(byte[] content);
    }

    private String safeFileName(String originalFilename) {
        String fileName = originalFilename == null ? "cv.pdf" : originalFilename;
        fileName = fileName.replace('\\', '/');
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1).trim();
        return fileName.isBlank() ? "cv.pdf" : fileName;
    }
}
