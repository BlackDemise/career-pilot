package blackdemise.cp.interview;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blackdemise.cp.interview.entity.InterviewIntegrityEvent;

public interface InterviewIntegrityEventRepository extends JpaRepository<InterviewIntegrityEvent, UUID> {
}