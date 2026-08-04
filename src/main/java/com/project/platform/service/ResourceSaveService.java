package com.project.platform.service;

import com.project.platform.dto.SaveResourceRequest;

/**
 * 资源保存服务
 */
public interface ResourceSaveService {
    
    /**
     * 保存用户搜索的资源
     * 
     * @param userId 用户ID
     * @param request 保存资源请求
     * @return 保存是否成功
     */
    boolean saveUserResources(String userId, SaveResourceRequest request);
    
    /**
     * 获取用户保存的资源列表
     * 
     * @param userId 用户ID
     * @param keyword 搜索关键词(可选)
     * @return 资源列表JSON字符串
     */
    String getUserSavedResources(String userId, String keyword);
    
    /**
     * 删除用户保存的资源
     * 
     * @param userId 用户ID
     * @param resourceId 资源ID
     * @return 删除是否成功
     */
    boolean deleteUserResource(String userId, Long resourceId);
}
