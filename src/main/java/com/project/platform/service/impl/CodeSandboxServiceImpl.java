package com.project.platform.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.project.platform.dto.CodeSandboxRequest;
import com.project.platform.service.CodeSandboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

@Service
@Slf4j
public class CodeSandboxServiceImpl implements CodeSandboxService {

    private static final int TIMEOUT_SECONDS = 10;
    private static final int JAVA_COMPILE_TIMEOUT_SECONDS = 20;

    private static final Set<String> PYTHON_BLOCKED = Set.of(
            "subprocess", "socket", "shutil", "requests", "urllib",
            "http", "ftplib", "telnetlib", "smtplib"
    );

    private static final Set<String> JAVA_BLOCKED = Set.of(
            "Runtime.getRuntime", "ProcessBuilder", "java.net.",
            "FileInputStream", "FileOutputStream",
            "new Socket", "ServerSocket", "ClassLoader",
            "Desktop.", "System.exit"
    );

    @Override
    public Map<String, Object> execute(CodeSandboxRequest request) {
        String language = request.getLanguage();
        if (!"python".equals(language) && !"java".equals(language)) {
            return errorResult("不支持的语言: " + language);
        }

        String safetyError = validateSafety(request.getCode(), language);
        if (safetyError != null) {
            return errorResult(safetyError);
        }

        try {
            Path workDir = Files.createTempDirectory("codesandbox_");
            try {
                if ("python".equals(language)) {
                    return executePython(request, workDir);
                } else {
                    return executeJava(request, workDir);
                }
            } finally {
                deleteRecursively(workDir);
            }
        } catch (Exception e) {
            log.error("代码沙箱执行异常", e);
            return errorResult("沙箱执行异常: " + e.getMessage());
        }
    }

    // ==================== Python ====================

    private Map<String, Object> executePython(CodeSandboxRequest request, Path workDir) throws Exception {
        String testCasesJson = JSONUtil.toJsonStr(request.getTestCases());
        String runnerCode = buildPythonRunner(request, testCasesJson);
        Path scriptFile = workDir.resolve("main.py");
        Files.writeString(scriptFile, runnerCode);

        String pythonCmd = findPythonCommand();
        if (pythonCmd == null) {
            return errorResult("服务器未安装 Python3，无法执行 Python 代码。");
        }

        String output = runProcess(workDir, TIMEOUT_SECONDS, pythonCmd, scriptFile.toString());
        return parseRunnerOutput(output);
    }

    private String buildPythonRunner(CodeSandboxRequest request, String testCasesJson) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getCode()).append("\n\n");
        sb.append("import json\nimport sys\n\n");
        sb.append("test_cases = ").append(testCasesJson).append("\n\n");

        if ("class".equals(request.getMode())) {
            String clsName = request.getClassName();
            sb.append("try:\n");
            sb.append("    _cls = ").append(clsName).append("\n");
            sb.append("except NameError:\n");
            sb.append("    print(json.dumps({'error': '未找到类 ").append(clsName).append("'}))\n");
            sb.append("    sys.exit(0)\n\n");
            sb.append("results = []\n");
            sb.append("for tc in test_cases:\n");
            sb.append("    try:\n");
            sb.append("        _inst = _cls()\n");
            sb.append("        _calls = tc.get('calls', [])\n");
            sb.append("        _actual = []\n");
            sb.append("        for _call in _calls:\n");
            sb.append("            _method = getattr(_inst, _call['method'], None)\n");
            sb.append("            if _method is None:\n");
            sb.append("                raise Exception('missing method ' + _call['method'])\n");
            sb.append("            _result = _method(*_call.get('args', []))\n");
            sb.append("            _actual.append(_result)\n");
            sb.append("        results.append({'id': tc['id'], 'actualValue': _actual})\n");
            sb.append("    except Exception as _e:\n");
            sb.append("        results.append({'id': tc['id'], 'error': str(_e)})\n");
        } else {
            String fnName = request.getFunctionName();
            sb.append("try:\n");
            sb.append("    _fn = ").append(fnName).append("\n");
            sb.append("except NameError:\n");
            sb.append("    print(json.dumps({'error': '未找到函数 ").append(fnName).append("'}))\n");
            sb.append("    sys.exit(0)\n\n");
            sb.append("results = []\n");
            sb.append("for tc in test_cases:\n");
            sb.append("    try:\n");
            sb.append("        _args = tc.get('args', [])\n");
            sb.append("        _actual = _fn(*_args)\n");
            if (request.getReturnArgIndex() != null) {
                sb.append("        if _actual is None:\n");
                sb.append("            _actual = _args[").append(request.getReturnArgIndex()).append("]\n");
            }
            sb.append("        results.append({'id': tc['id'], 'actualValue': _actual})\n");
            sb.append("    except Exception as _e:\n");
            sb.append("        results.append({'id': tc['id'], 'error': str(_e)})\n");
        }

        sb.append("\nprint('__RESULTS__')\nprint(json.dumps(results))\n");
        return sb.toString();
    }

    // ==================== Java ====================

    private Map<String, Object> executeJava(CodeSandboxRequest request, Path workDir) throws Exception {
        String userClassName = extractClassName(request);
        Path solutionFile = workDir.resolve(userClassName + ".java");
        Files.writeString(solutionFile, request.getCode());

        String mainCode = buildJavaRunner(request, userClassName);
        Path mainFile = workDir.resolve("Main.java");
        Files.writeString(mainFile, mainCode);

        String javacCmd = findJavaCompiler();
        if (javacCmd == null) {
            return errorResult("服务器未安装 JDK，无法编译 Java 代码。");
        }

        String compileOutput = runProcess(workDir, JAVA_COMPILE_TIMEOUT_SECONDS,
                javacCmd, mainFile.toString(), solutionFile.toString());
        if (compileOutput != null && !compileOutput.isBlank()) {
            return errorResult("Java 编译错误:\n" + compileOutput.trim());
        }

        String javaCmd = findJavaCommand();
        if (javaCmd == null) {
            return errorResult("服务器未安装 Java 运行时。");
        }

        String output = runProcess(workDir, TIMEOUT_SECONDS, javaCmd, "-cp", workDir.toString(), "Main");
        return parseRunnerOutput(output);
    }

    private String extractClassName(CodeSandboxRequest request) {
        String code = request.getCode();
        Matcher m = Pattern.compile("class\\s+(\\w+)").matcher(code);
        if (m.find()) {
            return m.group(1);
        }
        return "function".equals(request.getMode())
                ? capitalize(request.getFunctionName())
                : capitalize(request.getClassName());
    }

    private String buildJavaRunner(CodeSandboxRequest request, String userClassName) {
        StringBuilder sb = new StringBuilder();
        sb.append("import java.util.*;\n\n");
        sb.append("public class Main {\n");
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        System.out.println(\"__RESULTS__\");\n");
        sb.append("        System.out.println(\"[\");\n");

        List<CodeSandboxRequest.TestCase> testCases = request.getTestCases();
        if ("class".equals(request.getMode())) {
            sb.append("        ").append(userClassName).append(" obj = new ").append(userClassName).append("();\n");
            for (int i = 0; i < testCases.size(); i++) {
                CodeSandboxRequest.TestCase tc = testCases.get(i);
                String comma = i < testCases.size() - 1 ? "," : "";
                generateClassTestCaseJava(sb, tc, comma);
            }
        } else {
            for (int i = 0; i < testCases.size(); i++) {
                CodeSandboxRequest.TestCase tc = testCases.get(i);
                String comma = i < testCases.size() - 1 ? "," : "";
                generateFunctionTestCaseJava(sb, tc, request.getFunctionName(), userClassName,
                        request.getReturnArgIndex(), comma);
            }
        }

        sb.append("        System.out.println(\"]\");\n");
        sb.append("    }\n\n");
        sb.append(TO_JSON_METHOD);
        sb.append(ESCAPE_JSON_METHOD);
        sb.append(CALL_METHOD_METHOD);
        sb.append("}\n");
        return sb.toString();
    }

    private void generateFunctionTestCaseJava(StringBuilder sb, CodeSandboxRequest.TestCase tc,
                                               String functionName, String userClassName,
                                               Integer returnArgIndex, String comma) {
        String tcId = tc.getId();
        sb.append("        try {\n");
        sb.append("            ").append(userClassName).append(" _obj = new ").append(userClassName).append("();\n");

        // Build args
        List<Object> args = tc.getArgs();
        sb.append("            Object _actual = _obj.").append(functionName).append("(");
        for (int j = 0; j < args.size(); j++) {
            if (j > 0) sb.append(", ");
            sb.append(toJavaLiteral(args.get(j)));
        }
        sb.append(");\n");

        if (returnArgIndex != null) {
            sb.append("            if (_actual == null) _actual = _args[").append(returnArgIndex).append("];\n");
        }

        sb.append("            System.out.println(\"{\\\"id\\\":\\\"").append(tcId)
                .append("\\\",\\\"actualValue\\\":\" + toJson(_actual) + \"}\"").append(comma).append(");\n");
        sb.append("        } catch (Exception _e) {\n");
        sb.append("            System.out.println(\"{\\\"id\\\":\\\"").append(tcId)
                .append("\\\",\\\"error\\\":\\\"\" + escapeJson(_e.getMessage()) + \"\\\"}\"").append(comma).append(");\n");
        sb.append("        }\n");
    }

    private void generateClassTestCaseJava(StringBuilder sb, CodeSandboxRequest.TestCase tc, String comma) {
        String tcId = tc.getId();
        sb.append("        try {\n");
        sb.append("            java.util.List<Object> _results = new java.util.ArrayList<>();\n");

        List<CodeSandboxRequest.MethodCall> calls = tc.getCalls();
        if (calls != null) {
            for (CodeSandboxRequest.MethodCall call : calls) {
                sb.append("            _results.add(callMethod(obj, \"")
                        .append(call.getMethod()).append("\"");
                List<Object> callArgs = call.getArgs();
                if (callArgs != null && !callArgs.isEmpty()) {
                    for (Object arg : callArgs) {
                        sb.append(", ").append(toJavaLiteral(arg));
                    }
                }
                sb.append("));\n");
            }
        }

        sb.append("            System.out.println(\"{\\\"id\\\":\\\"").append(tcId)
                .append("\\\",\\\"actualValue\\\":\" + toJson(_results) + \"}\"").append(comma).append(");\n");
        sb.append("        } catch (Exception _e) {\n");
        sb.append("            System.out.println(\"{\\\"id\\\":\\\"").append(tcId)
                .append("\\\",\\\"error\\\":\\\"\" + escapeJson(_e.getMessage()) + \"\\\"}\"").append(comma).append(");\n");
        sb.append("        }\n");
    }

    // ==================== Java Literal Generator ====================

    private String toJavaLiteral(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Number) return obj.toString();
        if (obj instanceof Boolean) return obj.toString();
        if (obj instanceof String s) return "\"" + escapeJavaString(s) + "\"";
        if (obj instanceof JSONArray arr) {
            StringBuilder sb = new StringBuilder("new Object[]{");
            for (int i = 0; i < arr.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(toJavaLiteral(arr.get(i)));
            }
            sb.append("}");
            return sb.toString();
        }
        return "null";
    }

    // ==================== Java Helper Methods (embedded in runner) ====================

    private static final String TO_JSON_METHOD = """
            static String toJson(Object obj) {
                if (obj == null) return "null";
                if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
                if (obj instanceof String) return "\\"" + escapeJson((String)obj) + "\\"";
                if (obj instanceof Object[] arr) {
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < arr.length; i++) {
                        if (i > 0) sb.append(",");
                        sb.append(toJson(arr[i]));
                    }
                    sb.append("]");
                    return sb.toString();
                }
                if (obj instanceof java.util.List<?> list) {
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(toJson(list.get(i)));
                    }
                    sb.append("]");
                    return sb.toString();
                }
                if (obj instanceof int[] arr) {
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < arr.length; i++) {
                        if (i > 0) sb.append(",");
                        sb.append(arr[i]);
                    }
                    sb.append("]");
                    return sb.toString();
                }
                return "\\"" + escapeJson(obj.toString()) + "\\"";
            }
            """;

    private static final String ESCAPE_JSON_METHOD = """
            static String escapeJson(String s) {
                if (s == null) return "";
                StringBuilder sb = new StringBuilder();
                for (char c : s.toCharArray()) {
                    switch (c) {
                        case '"': sb.append("\\\\\\""); break;
                        case '\\\\': sb.append("\\\\\\\\"); break;
                        case '\\n': sb.append("\\\\n"); break;
                        case '\\r': sb.append("\\\\r"); break;
                        case '\\t': sb.append("\\\\t"); break;
                        default: sb.append(c);
                    }
                }
                return sb.toString();
            }
            """;

    private static final String CALL_METHOD_METHOD = """
            static Object callMethod(Object obj, String methodName, Object... args) throws Exception {
                for (java.lang.reflect.Method m : obj.getClass().getMethods()) {
                    if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                        return m.invoke(obj, args);
                    }
                }
                throw new NoSuchMethodException(methodName);
            }
            """;

    // ==================== Process Execution ====================

    private String runProcess(Path workDir, int timeoutSeconds, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            if (!finished) {
                process.destroyForcibly();
                return "__ERROR__: 执行超时（超过 " + timeoutSeconds + " 秒）";
            }
        }
        return output.toString();
    }

    // ==================== Output Parsing ====================

    private Map<String, Object> parseRunnerOutput(String output) {
        if (output == null || output.isBlank()) {
            return errorResult("程序无输出");
        }
        if (output.startsWith("__ERROR__:")) {
            return errorResult(output.substring("__ERROR__:".length()).trim());
        }

        String[] parts = output.split("__RESULTS__");
        if (parts.length < 2) {
            String trimmed = output.trim();
            if (trimmed.startsWith("{")) {
                try {
                    JSONObject obj = JSONUtil.parseObj(trimmed);
                    if (obj.containsKey("error")) {
                        return errorResult(obj.getStr("error"));
                    }
                } catch (Exception ignored) {}
            }
            return errorResult("程序执行异常:\n" + output.trim());
        }

        String jsonPart = parts[1].trim();
        int arrayStart = jsonPart.indexOf('[');
        int arrayEnd = jsonPart.lastIndexOf(']');
        if (arrayStart < 0 || arrayEnd < 0) {
            return errorResult("程序输出格式错误:\n" + jsonPart);
        }
        jsonPart = jsonPart.substring(arrayStart, arrayEnd + 1);

        try {
            JSONArray results = JSONUtil.parseArray(jsonPart);
            List<Map<String, Object>> outputs = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                JSONObject item = results.getJSONObject(i);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", item.getStr("id"));
                if (item.containsKey("error")) {
                    entry.put("error", item.getStr("error"));
                } else {
                    entry.put("actualValue", item.get("actualValue"));
                }
                outputs.add(entry);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("outputs", outputs);
            result.put("detail", "已在服务端沙箱中执行代码，并按测试用例比对输出。");
            return result;
        } catch (Exception e) {
            log.error("解析执行结果失败: {}", jsonPart, e);
            return errorResult("解析执行结果失败: " + e.getMessage());
        }
    }

    private Map<String, Object> errorResult(String error) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("error", error);
        return result;
    }

    // ==================== Safety ====================

    private String validateSafety(String code, String language) {
        if (code == null || code.isBlank()) {
            return "代码不能为空";
        }
        if ("python".equals(language)) {
            for (String blocked : PYTHON_BLOCKED) {
                if (code.contains("import " + blocked) || code.contains("from " + blocked)) {
                    return "检测到不允许的 Python 模块: " + blocked;
                }
            }
            if (code.contains("while True") || code.contains("while(True)")) {
                return "检测到无限循环，不允许执行";
            }
        } else if ("java".equals(language)) {
            for (String blocked : JAVA_BLOCKED) {
                if (code.contains(blocked)) {
                    return "检测到不允许的 Java API: " + blocked;
                }
            }
            if (code.contains("while (true)") || code.contains("while(true)")) {
                return "检测到无限循环，不允许执行";
            }
        }
        return null;
    }

    // ==================== Command Detection ====================

    private String findPythonCommand() {
        for (String cmd : new String[]{"python3", "python"}) {
            try {
                Process p = new ProcessBuilder(cmd, "--version").start();
                if (p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0) {
                    return cmd;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String findJavaCompiler() {
        try {
            Process p = new ProcessBuilder("javac", "-version").start();
            if (p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0) {
                return "javac";
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String findJavaCommand() {
        try {
            Process p = new ProcessBuilder("java", "-version").start();
            if (p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0) {
                return "java";
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ==================== Utilities ====================

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String escapeJavaString(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private void deleteRecursively(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (var stream = Files.walk(path)) {
                    stream.sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try { Files.delete(p); } catch (Exception ignored) {}
                            });
                }
            }
        } catch (Exception ignored) {}
    }
}
