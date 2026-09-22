package blackdemise.cp.cv;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import blackdemise.cp.cv.dto.CvRequirement;
import blackdemise.cp.cv.dto.CvRequirementMatch;

class CvMatchScoreCalculatorTest {

    @Test
    void calculatesWeightedOverallAndSectionScoresFromRequirementJudgments() {
        List<CvRequirement> requirements = List.of(
                requirement("java", RequirementCategory.REQUIRED, "Skills"),
                requirement("spring", RequirementCategory.PREFERRED, "Skills"),
                requirement("degree", RequirementCategory.OPTIONAL, "Education"));
        List<CvRequirementMatch> matches = List.of(
                match("java", CvMatchStatus.SUPPORTED, "Skills"),
                match("spring", CvMatchStatus.PARTIALLY_SUPPORTED, "Skills"),
                match("degree", CvMatchStatus.NOT_SUPPORTED, "Education"));

        var result = CvMatchScoreCalculator.calculate(requirements, matches);

        assertThat(result.overallScore()).isEqualTo(70);
        assertThat(result.sectionScores()).extracting("section")
                .containsExactly("Education", "Skills");
        assertThat(result.sectionScores().get(1).score()).isEqualTo(81);
        assertThat(result.sectionScores().get(1).matchedRequirementIds())
                .containsExactly("java", "spring");
        assertThat(result.sectionScores().get(0).missingRequirementIds()).containsExactly("degree");
    }

    private CvRequirement requirement(String id, RequirementCategory category, String section) {
        return new CvRequirement(id, id, category, 0.9, "test", id, section);
    }

    private CvRequirementMatch match(String id, CvMatchStatus status, String section) {
        return new CvRequirementMatch(id, status, section, null, List.of(), 0.9, null);
    }
}