package blackdemise.cp.user;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import blackdemise.cp.common.exception.NotFoundException;
import blackdemise.cp.user.dto.UserProfileRequest;
import blackdemise.cp.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse get(UUID userId) {
        return toResponse(findUser(userId));
    }

    @Transactional
    public UserProfileResponse update(UUID userId, UserProfileRequest request) {
        User user = findUser(userId);
        user.setPreferredLanguage(normalize(request.preferredLanguage()));
        user.setResponseStyle(normalize(request.responseStyle()));
        user.setTechnicalBackground(normalize(request.technicalBackground()));
        user.setCareerGoal(normalize(request.careerGoal()));
        user.setCustomInstructions(normalize(request.customInstructions()));
        return toResponse(userRepository.save(user));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(user.getId(), user.getPreferredLanguage(), user.getResponseStyle(),
                user.getTechnicalBackground(), user.getCareerGoal(), user.getCustomInstructions());
    }
}