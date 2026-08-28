package blackdemise.cp.user;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import blackdemise.cp.common.ApiResponse;
import blackdemise.cp.security.jwt.JwtUserPrincipal;
import blackdemise.cp.user.dto.UserProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users/me/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<ApiResponse> get(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(200, "Profile retrieved",
                userProfileService.get(userId(authentication))));
    }

    @PutMapping
    public ResponseEntity<ApiResponse> update(Authentication authentication,
            @Valid @RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(200, "Profile updated",
                userProfileService.update(userId(authentication), request)));
    }

    private UUID userId(Authentication authentication) {
        return ((JwtUserPrincipal) authentication.getPrincipal()).id();
    }
}