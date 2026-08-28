package blackdemise.cp.interview.dto;

import java.util.UUID;

public record InterviewTopicCatalogItem(UUID id, String code, String label, String phase, boolean required,
        boolean allowRepeat, int selectionWeight) {
}