package com.project.platform.service;

import com.project.platform.entity.ResourcePush;

import java.util.List;
import java.util.Map;

public interface ResourcePushService {

    /**
     * 搜索资源并推送到指定课程 (追加模式，不覆盖已有资源)
     *
     * @param courseId   课程ID
     * @param chapterId  章节ID (可选)
     * @param sectionId  小节ID (可选)
     * @param keyword    搜索关键词 (可选，为空则使用课程名)
     * @return 本次新推送的资源列表
     */
    List<ResourcePush> searchAndPush(Integer courseId, Integer chapterId, Integer sectionId, String keyword);

    /**
     * 获取课程的所有推送资源
     */
    List<ResourcePush> listByCourseId(Integer courseId);

    /**
     * 按类型获取课程推送资源
     */
    List<ResourcePush> listByCourseIdAndType(Integer courseId, String resourceType);

    List<ResourcePush> listContextualResources(Integer studentId, Integer courseId, Integer chapterId,
                                               Integer sectionId, String activeKnowledgePoint);

    String buildContextualSearchKeyword(Integer courseId, Integer chapterId, Integer sectionId,
                                        String activeKnowledgePoint);

    /**
     * 删除指定推送资源
     */
    void removeByIds(List<Integer> ids);
}
