package com.project.platform.service;

import com.project.platform.entity.Section;
import com.project.platform.vo.PageVO;

import java.util.List;
import java.util.Map;

/**
 * 章节小节服务类
 */
public interface SectionService {

    /**
     * 小节学习完成
     */
    void learnComplete(Integer userId, Integer sectionId);

    PageVO<Section> page(Map<String, Object> query, Integer pageNum, Integer pageSize);

    Section selectById(Integer id);

    List<Section> listByChapterId(Integer chapterId);

    void insert(Section entity);

    void updateById(Section entity);

    void removeByIds(List<Integer> ids);
}
