package com.project.platform.service;

import com.project.platform.dto.CodeSandboxRequest;
import java.util.Map;

public interface CodeSandboxService {
    Map<String, Object> execute(CodeSandboxRequest request);
}
