package com.project.platform.dto.learning;

import lombok.Data;

@Data
public class ResourceSummaryDTO {
    private int totalResourceCount;
    private int contextualResourceCount;
    private int remedialResourceCount;
    private int reviewedResourceCount;
}
