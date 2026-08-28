package blackdemise.cp.user;

import blackdemise.cp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "preferred_language", length = 100)
    private String preferredLanguage;

    @Column(name = "response_style", length = 100)
    private String responseStyle;

    @Column(name = "technical_background", columnDefinition = "TEXT")
    private String technicalBackground;

    @Column(name = "career_goal", columnDefinition = "TEXT")
    private String careerGoal;

    @Column(name = "custom_instructions", columnDefinition = "TEXT")
    private String customInstructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
