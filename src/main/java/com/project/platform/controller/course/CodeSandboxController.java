package com.project.platform.controller.course;

import com.project.platform.dto.CodeSandboxRequest;
import com.project.platform.service.CodeSandboxService;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/code-sandbox")
@CrossOrigin
@Slf4j
public class CodeSandboxController {

    @Resource
    private CodeSandboxService codeSandboxService;

    @PostMapping("/execute")
    public ResponseVO<Map<String, Object>> execute(@RequestBody CodeSandboxRequest request) {
        try {
            log.info("代码沙箱执行请求: language={}, mode={}", request.getLanguage(), request.getMode());
            Map<String, Object> result = codeSandboxService.execute(request);
            if (result.containsKey("error")) {
                return ResponseVO.error((String) result.get("error"));
            }
            return ResponseVO.ok(result);
        } catch (Exception e) {
            log.error("代码沙箱执行失败", e);
            return ResponseVO.error("沙箱执行失败: " + e.getMessage());
        }
    }
}
