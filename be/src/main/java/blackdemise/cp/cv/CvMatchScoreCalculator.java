package blackdemise.cp.cv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import blackdemise.cp.cv.dto.CvRequirement;
import blackdemise.cp.cv.dto.CvRequirementMatch;
import blackdemise.cp.cv.dto.CvSectionScore;

public final class CvMatchScoreCalculator {

    private CvMatchScoreCalculator() {
    }

    public static Score calculate(List<CvRequirement> requirements, List<CvRequirementMatch> matches) {
        Map<String, CvRequirementMatch> matchesByRequirement = new HashMap<>();
        matches.forEach(match -> matchesByRequirement.put(match.requirementId(), match));

        double weightedCoverage = 0;
        double totalWeight = 0;
        Map<String, SectionAccumulator> sections = new HashMap<>();
        for (CvRequirement requirement : requirements) {
            double weight = categoryWeight(requirement.category());
            CvRequirementMatch match = matchesByRequirement.get(requirement.id());
            double coverage = match == null ? 0 : coverage(match.status());
            weightedCoverage += weight * coverage;
            totalWeight += weight;
            String section = normalizeSection(requirement.section());
            sections.computeIfAbsent(section, ignored -> new SectionAccumulator())
                    .add(requirement.id(), weight, coverage, match == null ? CvMatchStatus.NOT_SUPPORTED : match.status());
        }

        List<CvSectionScore> sectionScores = sections.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue().toScore(entry.getKey()))
                .toList();
        int score = totalWeight == 0 ? 0 : (int) Math.round(weightedCoverage / totalWeight * 100);
        return new Score(score, sectionScores);
    }

    private static double categoryWeight(RequirementCategory category) {
        return switch (category) {
            case REQUIRED -> 1.0;
            case PREFERRED -> 0.6;
            case OPTIONAL -> 0.25;
        };
    }

    private static double coverage(CvMatchStatus status) {
        return switch (status) {
            case SUPPORTED -> 1.0;
            case PARTIALLY_SUPPORTED -> 0.5;
            case UNCLEAR -> 0.25;
            case NOT_SUPPORTED -> 0.0;
        };
    }

    private static String normalizeSection(String section) {
        return section == null || section.isBlank() ? "OTHER" : section.trim();
    }

    public record Score(int overallScore, List<CvSectionScore> sectionScores) {
    }

    private static final class SectionAccumulator {
        private double weightedCoverage;
        private double totalWeight;
        private final List<String> matched = new ArrayList<>();
        private final List<String> missing = new ArrayList<>();

        private void add(String requirementId, double weight, double coverage, CvMatchStatus status) {
            weightedCoverage += weight * coverage;
            totalWeight += weight;
            if (status == CvMatchStatus.SUPPORTED || status == CvMatchStatus.PARTIALLY_SUPPORTED) {
                matched.add(requirementId);
            } else {
                missing.add(requirementId);
            }
        }

        private CvSectionScore toScore(String section) {
            int score = totalWeight == 0 ? 0 : (int) Math.round(weightedCoverage / totalWeight * 100);
            return new CvSectionScore(section, score, List.copyOf(matched), List.copyOf(missing), List.of());
        }
    }
}