package blackdemise.cp.cv;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import blackdemise.cp.common.ApiResponse;
import blackdemise.cp.cv.dto.CvAnalysisResponse;
import blackdemise.cp.cv.dto.CvAnalysisJobResponse;
import blackdemise.cp.cv.dto.CvExtractionJobResponse;
import blackdemise.cp.cv.dto.CvJdMatchRequest;
import blackdemise.cp.cv.dto.CvResponse;
import blackdemise.cp.security.jwt.JwtUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cvs")
@RequiredArgsConstructor
public class CvController {

    private final CvService cvService;
    private final CvExtractionJobService cvExtractionJobService;
    private final CvAnalysisJobService cvAnalysisJobService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse> upload(Authentication authentication,
            @RequestPart("file") MultipartFile file) {
        CvResponse result = cvService.upload(userId(authentication), file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "CV uploaded", result));
    }

    @PostMapping("/{cvId}/analyses/review")
    public ResponseEntity<ApiResponse> review(Authentication authentication, @PathVariable UUID cvId) {
        CvAnalysisResponse result = cvService.review(userId(authentication), cvId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "CV review completed", result));
    }

    @PostMapping("/{cvId}/analyses/jd-match")
    public ResponseEntity<ApiResponse> matchJobDescription(Authentication authentication, @PathVariable UUID cvId,
            @Valid @RequestBody CvJdMatchRequest request) {
        CvAnalysisJobResponse result = cvAnalysisJobService.startJdMatch(userId(authentication), cvId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(HttpStatus.ACCEPTED.value(), "CV and job description analysis queued", result));
    }

    @GetMapping("/{cvId}/analyses")
    public ResponseEntity<ApiResponse> listAnalyses(Authentication authentication, @PathVariable UUID cvId) {
        List<CvAnalysisResponse> result = cvService.listAnalyses(userId(authentication), cvId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "CV analyses retrieved", result));
    }

    @PostMapping("/{cvId}/extraction")
    public ResponseEntity<ApiResponse> startExtraction(Authentication authentication, @PathVariable UUID cvId) {
        CvExtractionJobResponse result = cvExtractionJobService.start(userId(authentication), cvId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(HttpStatus.ACCEPTED.value(), "CV extraction queued", result));
    }

    @GetMapping("/extraction-jobs/{jobId}")
    public ResponseEntity<ApiResponse> getExtractionJob(Authentication authentication, @PathVariable UUID jobId) {
        CvExtractionJobResponse result = cvExtractionJobService.get(userId(authentication), jobId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "CV extraction job retrieved", result));
    }

    @GetMapping("/analysis-jobs/{jobId}")
    public ResponseEntity<ApiResponse> getAnalysisJob(Authentication authentication, @PathVariable UUID jobId) {
        CvAnalysisJobResponse result = cvAnalysisJobService.get(userId(authentication), jobId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "CV analysis job retrieved", result));
    }

    private UUID userId(Authentication authentication) {
        return ((JwtUserPrincipal) authentication.getPrincipal()).id();
    }
}
