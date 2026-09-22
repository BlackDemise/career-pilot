package blackdemise.cp.cv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import tools.jackson.databind.json.JsonMapper;

import blackdemise.cp.ai.AiService;
import blackdemise.cp.ai.prompt.PromptTemplateService;
import blackdemise.cp.common.exception.BadRequestException;
import blackdemise.cp.common.exception.NotFoundException;
import blackdemise.cp.cv.dto.CvJdMatchRequest;
import blackdemise.cp.cv.dto.CvJdMatchResult;
import blackdemise.cp.cv.entity.Cv;
import blackdemise.cp.user.User;

class CvServiceTest {

    private final CvRepository cvRepository = mock(CvRepository.class);
    private final CvAnalysisRepository cvAnalysisRepository = mock(CvAnalysisRepository.class);
    private final blackdemise.cp.user.UserRepository userRepository = mock(blackdemise.cp.user.UserRepository.class);
    private final AiService aiService = mock(AiService.class);
    private final PromptTemplateService promptTemplateService = mock(PromptTemplateService.class);
    private final CvService cvService = new CvService(cvRepository, cvAnalysisRepository, userRepository, aiService,
            promptTemplateService, JsonMapper.builder().build());

    private final UUID userId = UUID.randomUUID();
    private final UUID cvId = UUID.randomUUID();
    private User user;
    private Cv cv;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(userId);
        cv = new Cv();
        cv.setId(cvId);
        cv.setUser(user);
        cv.setFileName("resume.pdf");
        cv.setExtractedText("Java developer with Spring experience");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cvRepository.findById(cvId)).thenReturn(Optional.of(cv));
        when(promptTemplateService.render(eq("cv-jd-requirements"), any(Map.class))).thenReturn("requirements prompt");
        when(promptTemplateService.render(eq("cv-jd-requirement-matching"), any(Map.class)))
            .thenReturn("matching prompt");
        when(aiService.generate(null, "requirements prompt")).thenReturn(
            "{\"requirements\":[{\"id\":\"java\",\"requirement\":\"Java\","
                + "\"category\":\"REQUIRED\",\"categoryConfidence\":0.95,"
                + "\"categoryRationale\":\"required skill\",\"sourceText\":\"Java\","
                + "\"section\":\"Skills\"}]}");
        when(aiService.generate(null, "matching prompt")).thenReturn(
            "{\"requirementMatches\":[{\"requirementId\":\"java\",\"status\":\"SUPPORTED\","
                + "\"section\":\"Skills\",\"evidenceQuote\":\"Java developer\","
                + "\"sourceBlockIds\":[],\"confidence\":0.9}],\"matchedSkills\":[\"Java\"],"
                + "\"missingSkills\":[],\"experienceGaps\":[],\"recommendations\":[]}");
    }

    @Test
    void upload_extractsPdfTextAndPersistsCv() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfBytes("Java Spring"));
        when(cvRepository.save(any(Cv.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = cvService.upload(userId, file);

        assertThat(result.fileName()).isEqualTo("resume.pdf");
        assertThat(result.extractedText()).contains("Java Spring");
        verify(cvRepository).save(any(Cv.class));
    }

    @Test
    void upload_extractsDocxTextAndPersistsCv() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxBytes("Java Spring"));
        when(cvRepository.save(any(Cv.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = cvService.upload(userId, file);

        assertThat(result.fileName()).isEqualTo("resume.docx");
        assertThat(result.extractedText()).contains("Java Spring");
        verify(cvRepository).save(any(Cv.class));
    }

    @Test
    void uploadRejectsContentThatDoesNotMatchTheExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf",
                docxBytes("Java Spring"));

        assertThatThrownBy(() -> cvService.upload(userId, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The CV content does not match its file extension");
        verify(cvRepository, never()).save(any(Cv.class));
    }

    @Test
    void matchJobDescriptionParsesTypedResultAndPersistsJson() {
        when(cvAnalysisRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = cvService.matchJobDescription(userId, cvId, new CvJdMatchRequest("  Java backend role  "));

        assertThat(result.result()).isInstanceOf(CvJdMatchResult.class);
        assertThat(((CvJdMatchResult) result.result()).matchScore()).isEqualTo(100);
        assertThat(((CvJdMatchResult) result.result()).requirements()).hasSize(1);
        assertThat(((CvJdMatchResult) result.result()).sectionScores()).hasSize(1);
        assertThat(result.jobDescription()).isEqualTo("Java backend role");
        verify(cvAnalysisRepository).save(any());
    }

    @Test
    void analysisRejectsCvOwnedByAnotherUser() {
        UUID otherUserId = UUID.randomUUID();

        assertThatThrownBy(() -> cvService.review(otherUserId, cvId)).isInstanceOf(NotFoundException.class);
        verify(aiService, never()).generate(any(), any());
    }

    private byte[] pdfBytes(String text) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            try (PDPageContentStream content = new PDPageContentStream(document, document.getPage(0))) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] docxBytes(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(text);
            document.write(output);
            return output.toByteArray();
        }
    }
}
