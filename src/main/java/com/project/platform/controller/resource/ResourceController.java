package com.project.platform.controller.resource;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.dto.SaveResourceRequest;
import com.project.platform.service.ResourceSaveService;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/resource")
public class ResourceController {


    @Resource
    private ResourceSaveService resourceSaveService;

    @Resource
    private Tools tools;

    /**
     * 保存用户搜索的资源
     */
    @PostMapping("/save")
    public ResponseVO<?> saveResource(@RequestBody SaveResourceRequest request) {
        String userId = tools.getCurrentUserId();
        log.info("=== 保存资源: userId={}, keyword={}", userId, request.getKeyword());

        boolean success = resourceSaveService.saveUserResources(userId, request);

        if (success) {
            return ResponseVO.ok("资源保存成功");
        } else {
            return ResponseVO.error("资源保存失败");
        }
    }

    /**
     * 获取用户保存的资源列表
     */
    @GetMapping("/list")
    public ResponseVO<?> getResourceList(@RequestParam(required = false) String keyword) {
        String userId = tools.getCurrentUserId();
        log.info("=== 获取用户资源列表: userId={}, keyword={}", userId, keyword);

        String resources = resourceSaveService.getUserSavedResources(userId, keyword);
        return ResponseVO.ok(resources);
    }

    /**
     * 删除用户保存的资源
     */
    @DeleteMapping("/{resourceId}")
    public ResponseVO<?> deleteResource(@PathVariable Long resourceId) {
        String userId = tools.getCurrentUserId();
        log.info("=== 删除用户资源: userId={}, resourceId={}", userId, resourceId);

        boolean success = resourceSaveService.deleteUserResource(userId, resourceId);

        if (success) {
            return ResponseVO.ok("资源删除成功");
        } else {
            return ResponseVO.error("资源删除失败");
        }
    }
}
