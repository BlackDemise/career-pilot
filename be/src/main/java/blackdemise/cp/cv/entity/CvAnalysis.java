package blackdemise.cp.cv.entity;

import blackdemise.cp.common.BaseEntity;
import blackdemise.cp.cv.CvAnalysisType;
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
@Table(name = "cv_analyses")
public class CvAnalysis extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    private Cv cv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CvAnalysisType type;

    // Only populated for JD_MATCH analyses.
    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String resultJson;
}
