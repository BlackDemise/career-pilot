package blackdemise.cp.cv.dto;

import java.util.List;

public record CvStructuredExtraction(
        List<CvSectionEntry> education,
        List<CvSectionEntry> experience,
        List<CvSectionEntry> projects,
        List<String> skills,
        List<String> certifications,
        List<String> languages) {

    public record CvSectionEntry(
            String title,
            String organization,
            String dateRange,
            String description,
            List<String> sourceBlockIds,
            String sourceText) {
    }
}