package blackdemise.cp.cv;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import blackdemise.cp.cv.dto.CvRequirementMatch;

class CvEvidenceVerifierTest {

    @Test
    void verifiesNormalizedEvidenceAndDowngradesUnsupportedPositiveClaims() {
        List<CvRequirementMatch> matches = List.of(
                new CvRequirementMatch("java", CvMatchStatus.SUPPORTED, "Skills", "Java   developer",
                        List.of(), 0.9, null),
                new CvRequirementMatch("cloud", CvMatchStatus.SUPPORTED, "Experience", "Cloud deployment",
                        List.of(), 0.9, null));

        var verified = CvEvidenceVerifier.verify("Java developer with Spring experience", matches);

        assertThat(verified.get(0).verification()).isEqualTo(EvidenceVerificationStatus.VERIFIED_NORMALIZED);
        assertThat(verified.get(0).status()).isEqualTo(CvMatchStatus.SUPPORTED);
        assertThat(verified.get(1).verification()).isEqualTo(EvidenceVerificationStatus.INVALID_REFERENCE);
        assertThat(verified.get(1).status()).isEqualTo(CvMatchStatus.UNCLEAR);
    }
}