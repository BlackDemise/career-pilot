package blackdemise.cp.cv;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blackdemise.cp.cv.entity.CvAnalysisJob;

public interface CvAnalysisJobRepository extends JpaRepository<CvAnalysisJob, UUID> {

    Optional<CvAnalysisJob> findByIdAndUserId(UUID id, UUID userId);
}