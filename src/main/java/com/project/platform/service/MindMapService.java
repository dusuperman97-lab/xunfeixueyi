package com.project.platform.service;

import com.project.platform.entity.MindMap;

import java.util.List;

/**
 * 思维导图服务接口
 */
public interface MindMapService {

    /**
     * 保存思维导图记录
     * @param mindMap 思维导图对象
     * @return 保存后的思维导图（包含生成的ID）
     */
    MindMap saveMindMap(MindMap mindMap);

    /**
     * 根据ID查询思维导图
     * @param id 思维导图ID
     * @return 思维导图对象
     */
    MindMap getMindMapById(Long id);

    /**
     * 查询用户的所有思维导图
     * @param userId 用户ID
     * @return 思维导图列表
     */
    List<MindMap> getUserMindMaps(Integer userId);

    /**
     * 根据章节ID查询思维导图
     * @param chapterId 章节ID
     * @return 思维导图列表
     */
    List<MindMap> getMindMapsByChapterId(Integer chapterId);

    /**
     * 根据小节ID查询思维导图
     * @param sectionId 小节ID
     * @return 思维导图列表
     */
    List<MindMap> getMindMapsBySectionId(Integer sectionId);

    /**
     * 更新思维导图标题
     * @param id 思维导图ID
     * @param title 新标题
     * @return 是否成功
     */
    boolean updateTitle(Long id, String title);

    /**
     * 删除思维导图
     * @param id 思维导图ID
     * @return 是否成功
     */
    boolean deleteMindMap(Long id);

    /**
     * 批量删除思维导图
     * @param ids ID列表（逗号分隔）
     * @return 删除数量
     */
    int deleteBatch(String ids);
}
