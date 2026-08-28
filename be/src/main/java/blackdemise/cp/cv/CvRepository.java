package blackdemise.cp.cv;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blackdemise.cp.cv.entity.Cv;

public interface CvRepository extends JpaRepository<Cv, UUID> {
}
