package blackdemise.cp.interview;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blackdemise.cp.interview.entity.InterviewRole;

public interface InterviewRoleRepository extends JpaRepository<InterviewRole, UUID> {
    List<InterviewRole> findByActiveTrueOrderByLabelAsc();
}