package com.project.platform.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleSnapshotDTO {
    private String module;
    private String status;
    private int count;
    private String actionLabel;
}
