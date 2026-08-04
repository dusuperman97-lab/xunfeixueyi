package com.project.platform.service;

import com.project.platform.dto.ChapterCompletionDTO;
import com.project.platform.entity.Chapter;
import com.project.platform.vo.PageVO;

import java.util.List;
import java.util.Map;

/**
 * 课程章节服务类
 */
public interface ChapterService {

    PageVO<Chapter> page(Map<String, Object> query, Integer pageNum, Integer pageSize);

    Chapter selectById(Integer id);

    List<Chapter> listByCourseId(Integer courseId);

    void insert(Chapter entity);

    void updateById(Chapter entity);

    void removeByIds(List<Integer> ids);

    Chapter selectByCourseIdAndChapterName(Integer courseId, String chapterName);

    /**
     * 获取章节完成情况统计
     */
    ChapterCompletionDTO getChapterCompletion(Integer chapterId);
}