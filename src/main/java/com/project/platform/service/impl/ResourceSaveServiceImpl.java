package com.project.platform.service.impl;

import cn.hutool.json.JSONUtil;
import com.project.platform.dto.SaveResourceRequest;
import com.project.platform.entity.SavedResource;
import com.project.platform.mapper.SavedResourceMapper;
import com.project.platform.service.ResourceSaveService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资源保存服务实现
 */
@Slf4j
@Service
public class ResourceSaveServiceImpl implements ResourceSaveService {
    
    @Resource
    private SavedResourceMapper savedResourceMapper;
    
    @Override
    public boolean saveUserResources(String userId, SaveResourceRequest request) {
        try {
            log.info("=== 开始保存用户资源: userId={}, keyword={}", userId, request.getKeyword());
            
            String resourceJson = JSONUtil.toJsonStr(request.getResourceData());
            
            // 保存搜索关键词对应的资源集合
            SavedResource savedResource = new SavedResource();
            savedResource.setUserId(Long.parseLong(userId));
            savedResource.setKeyword(request.getKeyword());
            savedResource.setResourceData(resourceJson);
            savedResource.setMemoryId(request.getMemoryId());
            savedResource.setCreateTime(LocalDateTime.now());
            savedResource.setUpdateTime(LocalDateTime.now());
            
            int result = savedResourceMapper.insert(savedResource);
            
            log.info("=== 用户资源保存成功: userId={}, resourceId={}", userId, savedResource.getId());
            return result > 0;
            
        } catch (Exception e) {
            log.error("保存用户资源失败", e);
            return false;
        }
    }
    
    @Override
    public String getUserSavedResources(String userId, String keyword) {
        try {
            log.info("=== 获取用户保存的资源: userId={}, keyword={}", userId, keyword);
            
            List<SavedResource> resources;
            if (keyword != null && !keyword.trim().isEmpty()) {
                resources = savedResourceMapper.selectByUserIdAndKeyword(Long.parseLong(userId), keyword);
            } else {
                resources = savedResourceMapper.selectByUserId(Long.parseLong(userId));
            }
            
            return JSONUtil.toJsonStr(resources);
            
        } catch (Exception e) {
            log.error("获取用户保存资源失败", e);
            return "[]";
        }
    }
    
    @Override
    public boolean deleteUserResource(String userId, Long resourceId) {
        try {
            log.info("=== 删除用户资源: userId={}, resourceId={}", userId, resourceId);
            
            int result = savedResourceMapper.deleteById(resourceId);
            
            log.info("=== 用户资源删除结果: {}", result > 0);
            return result > 0;
            
        } catch (Exception e) {
            log.error("删除用户资源失败", e);
            return false;
        }
    }
}
