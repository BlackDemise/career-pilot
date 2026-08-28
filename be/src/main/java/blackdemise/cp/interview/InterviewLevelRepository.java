package blackdemise.cp.interview;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blackdemise.cp.interview.entity.InterviewLevel;

public interface InterviewLevelRepository extends JpaRepository<InterviewLevel, UUID> {
    List<InterviewLevel> findByActiveTrueOrderByLabelAsc();
}