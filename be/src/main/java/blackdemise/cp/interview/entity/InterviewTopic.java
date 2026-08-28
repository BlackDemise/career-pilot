package blackdemise.cp.interview.entity;

import blackdemise.cp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "interview_topics")
public class InterviewTopic extends BaseEntity {
    @Column(nullable = false, unique = true, length = 64)
    private String code;
    @Column(nullable = false)
    private String label;
    @Column(nullable = false)
    private String phase;
    @Column(nullable = false)
    private boolean active = true;
}