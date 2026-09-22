package blackdemise.cp.cv;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blackdemise.cp.cv.entity.CvExtractionJob;

public interface CvExtractionJobRepository extends JpaRepository<CvExtractionJob, UUID> {

    Optional<CvExtractionJob> findByIdAndUserId(UUID id, UUID userId);
}