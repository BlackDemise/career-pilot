package blackdemise.cp.interview.entity;

import java.time.Instant;

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
@Table(name = "interview_integrity_events")
public class InterviewIntegrityEvent extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;
    @Column(nullable = false, length = 64)
    private String eventType;
    @Column(nullable = false)
    private Instant occurredAt;
    @Column(columnDefinition = "TEXT")
    private String metadata;
}