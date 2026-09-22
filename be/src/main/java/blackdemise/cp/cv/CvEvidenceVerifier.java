package blackdemise.cp.cv;

import java.util.List;

import blackdemise.cp.cv.dto.CvRequirementMatch;

public final class CvEvidenceVerifier {

    private CvEvidenceVerifier() {
    }

    public static List<CvRequirementMatch> verify(String cvText, List<CvRequirementMatch> matches) {
        return matches.stream().map(match -> verifyMatch(cvText, match)).toList();
    }

    private static CvRequirementMatch verifyMatch(String cvText, CvRequirementMatch match) {
        String quote = match.evidenceQuote();
        if (quote == null || quote.isBlank()) {
            return new CvRequirementMatch(match.requirementId(), downgrade(match.status()), match.section(), quote,
                    match.sourceBlockIds(), match.confidence(), EvidenceVerificationStatus.UNVERIFIED);
        }
        if (cvText.contains(quote)) {
            return new CvRequirementMatch(match.requirementId(), match.status(), match.section(), quote,
                    match.sourceBlockIds(), match.confidence(), EvidenceVerificationStatus.VERIFIED_EXACT);
        }
        if (normalize(cvText).contains(normalize(quote))) {
            return new CvRequirementMatch(match.requirementId(), match.status(), match.section(), quote,
                    match.sourceBlockIds(), match.confidence(), EvidenceVerificationStatus.VERIFIED_NORMALIZED);
        }
        return new CvRequirementMatch(match.requirementId(), downgrade(match.status()), match.section(), quote,
                match.sourceBlockIds(), match.confidence(), EvidenceVerificationStatus.INVALID_REFERENCE);
    }

    private static CvMatchStatus downgrade(CvMatchStatus status) {
        return status == CvMatchStatus.SUPPORTED || status == CvMatchStatus.PARTIALLY_SUPPORTED
                ? CvMatchStatus.UNCLEAR
                : status;
    }

    private static String normalize(String value) {
        return value.toLowerCase().replaceAll("\\s+", " ").trim();
    }
}