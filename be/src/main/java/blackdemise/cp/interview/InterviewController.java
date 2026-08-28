package blackdemise.cp.interview;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import blackdemise.cp.common.ApiResponse;
import blackdemise.cp.interview.dto.InterviewSetupRequest;
import blackdemise.cp.interview.dto.SubmitAnswerRequest;
import blackdemise.cp.security.jwt.JwtUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse> catalog(@RequestParam(required = false) UUID roleId,
            @RequestParam(required = false) UUID levelId) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Interview catalog retrieved",
                interviewService.catalog(roleId, levelId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(Authentication authentication,
            @Valid @RequestBody InterviewSetupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED.value(),
                "Interview created", interviewService.create(userId(authentication), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse> list(Authentication authentication) {
        List<?> result = interviewService.list(userId(authentication));
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Interviews retrieved", result));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse> get(Authentication authentication, @PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Interview retrieved",
                interviewService.get(userId(authentication), sessionId)));
    }

    @GetMapping("/{sessionId}/report")
    public ResponseEntity<ApiResponse> report(Authentication authentication, @PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Interview report generated",
                interviewService.finalReport(userId(authentication), sessionId)));
    }

    private UUID userId(Authentication authentication) {
        return ((JwtUserPrincipal) authentication.getPrincipal()).id();
    }
}