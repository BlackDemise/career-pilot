package blackdemise.cp.cv;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blackdemise.cp.cv.entity.CvAnalysis;

public interface CvAnalysisRepository extends JpaRepository<CvAnalysis, UUID> {

    List<CvAnalysis> findByCvId(UUID cvId);
}
