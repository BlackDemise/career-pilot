package blackdemise.cp.interview.entity;

import blackdemise.cp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "interview_catalog_rules")
public class InterviewCatalogRule extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private InterviewRole role;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id", nullable = false)
    private InterviewLevel level;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private InterviewTopic topic;
    @Column(nullable = false)
    private boolean required;
    @Column(nullable = false)
    private boolean allowRepeat;
    @Column(nullable = false)
    private int selectionWeight;
}