package com.project.platform.dto;

import lombok.Data;
import java.util.List;

@Data
public class CodeSandboxRequest {
    private String language;
    private String code;
    private String mode;
    private String functionName;
    private String className;
    private Integer returnArgIndex;
    private List<TestCase> testCases;

    @Data
    public static class TestCase {
        private String id;
        private List<Object> args;
        private List<MethodCall> calls;
        private Object expectedValue;
        private String compare;
    }

    @Data
    public static class MethodCall {
        private String method;
        private List<Object> args;
        private Object expected;
    }
}
