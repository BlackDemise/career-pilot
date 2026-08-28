package blackdemise.cp.interview.dto;

import java.util.List;

public record InterviewCatalogResponse(List<InterviewCatalogItem> roles, List<InterviewCatalogItem> levels,
        List<InterviewTopicCatalogItem> topics) {
}