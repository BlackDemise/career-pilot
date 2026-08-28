package blackdemise.cp.interview;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blackdemise.cp.interview.entity.InterviewCatalogRule;

public interface InterviewCatalogRuleRepository extends JpaRepository<InterviewCatalogRule, UUID> {
    List<InterviewCatalogRule> findByRoleIdAndLevelId(UUID roleId, UUID levelId);
}