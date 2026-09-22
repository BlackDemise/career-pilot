package blackdemise.cp.cv.entity;

import blackdemise.cp.common.BaseEntity;
import blackdemise.cp.cv.CvExtractionJobStatus;
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
@Table(name = "cv_extraction_jobs")
public class CvExtractionJob extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    private Cv cv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CvExtractionJobStatus status;

    @Column(nullable = false)
    private String stage;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}