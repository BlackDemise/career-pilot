package blackdemise.cp.interview.entity;

import blackdemise.cp.common.BaseEntity;
import blackdemise.cp.interview.InterviewStatus;
import blackdemise.cp.interview.InterviewPhase;
import blackdemise.cp.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "interview_sessions")
public class InterviewSession extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String level;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String difficulty;

    @Column(nullable = false)
    private Integer numQuestions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status;

    @Column(nullable = false)
    private Integer durationSeconds;

    @Column(nullable = false)
    private Integer minimumPrimaryQuestions;

    @Column(nullable = false)
    private Integer maximumPrimaryQuestions;

    @Column(nullable = false)
    private Integer primaryQuestionsAsked = 0;

    @Column(nullable = false)
    private Integer totalTurns = 0;

    @Column
    private java.time.Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column
    private InterviewPhase currentPhase;

    @Column
    private String selectedPlanJson;

    @Column
    private java.util.UUID currentQuestionId;
}
