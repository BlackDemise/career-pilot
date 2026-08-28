package blackdemise.cp.interview;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blackdemise.cp.interview.entity.InterviewAnswer;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, UUID> {

    Optional<InterviewAnswer> findByQuestionId(UUID questionId);
}
