package blackdemise.cp.interview;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blackdemise.cp.interview.entity.InterviewEvaluation;

public interface InterviewEvaluationRepository extends JpaRepository<InterviewEvaluation, UUID> {

    Optional<InterviewEvaluation> findByAnswerId(UUID answerId);
}
