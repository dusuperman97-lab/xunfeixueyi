package com.project.platform.service.impl;

import com.project.platform.dto.ChapterCompletionDTO;
import com.project.platform.dto.SectionCompletionDTO;
import com.project.platform.entity.Chapter;
import com.project.platform.entity.Section;
import com.project.platform.exception.CustomException;
import com.project.platform.mapper.ChapterMapper;
import com.project.platform.mapper.SectionMapper;
import com.project.platform.service.ChapterService;
import com.project.platform.service.QuestionService;
import com.project.platform.vo.PageVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 课程章节服务实现类
 */
@Service
public class ChapterServiceImpl implements ChapterService {
    @Resource
    private ChapterMapper chapterMapper;
    @Resource
    private SectionMapper sectionMapper;
    @Resource
    private QuestionService questionService;

    @Override
    public PageVO<Chapter> page(Map<String, Object> query, Integer pageNum, Integer pageSize) {
        PageVO<Chapter> page = new PageVO();
        List<Chapter> list = chapterMapper.queryPage((pageNum - 1) * pageSize, pageSize, query);
        page.setList(list);
        page.setTotal(chapterMapper.queryCount(query));
        return page;
    }

    @Override
    public Chapter selectById(Integer id) {
        Chapter chapter = chapterMapper.selectById(id);
        // 查询该章节下的小节
        if (chapter != null) {
            chapter.setSectionList(sectionMapper.listByChapterId(id));
        }
        return chapter;
    }

    @Override
    public List<Chapter> listByCourseId(Integer courseId) {
        List<Chapter> chapters = chapterMapper.listByCourseId(courseId);
        // 为每个章节加载小节列表
        for (Chapter chapter : chapters) {
            chapter.setSectionList(sectionMapper.listByChapterId(chapter.getId()));
        }
        return chapters;
    }

    @Override
    public void insert(Chapter entity) {
        check(entity);
        chapterMapper.insert(entity);
    }

    @Override
    public void updateById(Chapter entity) {
        check(entity);
        chapterMapper.updateById(entity);
    }

    private void check(Chapter entity) {
        // 校验课程是否存在
        if (entity.getCourseId() == null) {
            throw new CustomException("请选择所属课程");
        }
        // 校验同一课程下章节名称不能重复
        Chapter chapter = chapterMapper.selectByCourseIdAndName(entity.getCourseId(), entity.getChapterName());
        if (chapter != null && !chapter.getId().equals(entity.getId())) {
            throw new CustomException("该课程下已存在相同名称的章节");
        }
    }

    @Override
    @Transactional  // 删除章节时，级联删除其下所有小节
    public void removeByIds(List<Integer> ids) {
        // 先删除小节
        for (Integer chapterId : ids) {
            sectionMapper.removeByChapterId(chapterId);
        }
        // 再删除章节
        chapterMapper.removeByIds(ids);
    }

    @Override
    public Chapter selectByCourseIdAndChapterName(Integer courseId, String chapterName) {
        return chapterMapper.selectByCourseIdAndName(courseId,chapterName);
    }

    @Override
    public ChapterCompletionDTO getChapterCompletion(Integer chapterId) {
        // 1. 获取章节信息
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new CustomException("章节不存在，ID: " + chapterId);
        }

        // 2. 获取该章节下所有小节
        List<Section> sections = sectionMapper.listByChapterId(chapterId);
        int totalSections = sections.size();
        int completedSections = 0;

        // 3. 统计小节完成情况
        List<SectionCompletionDTO> sectionDTOs = new ArrayList<>();
        for (Section section : sections) {
            // 统计已完成的小节
            if (section.getIsCompleted() != null && section.getIsCompleted()) {
                completedSections++;
            }

            // 获取该小节的题目数量
            int questionCount = questionService.getBySectionId(section.getId()).size();

            // 构建小节完成DTO
            SectionCompletionDTO sectionDTO = new SectionCompletionDTO();
            sectionDTO.setSectionId(section.getId());
            sectionDTO.setSectionName(section.getSectionName());
            sectionDTO.setSortOrder(section.getSortOrder());
            sectionDTO.setIsCompleted(section.getIsCompleted());
            sectionDTO.setContentUrl(section.getContentUrl());
            sectionDTO.setQuestionCount(questionCount);
            sectionDTOs.add(sectionDTO);
        }

        // 4. 计算完成率
        double completionRate = totalSections > 0 ? (double) completedSections / totalSections * 100 : 0.0;
        completionRate = Math.round(completionRate * 100.0) / 100.0; // 保留两位小数

        // 5. 构建返回对象
        ChapterCompletionDTO dto = new ChapterCompletionDTO();
        dto.setChapterId(chapter.getId());
        dto.setChapterName(chapter.getChapterName());
        dto.setSortOrder(chapter.getSortOrder());
        dto.setTotalSections(totalSections);
        dto.setCompletedSections(completedSections);
        dto.setCompletionRate(completionRate);
        dto.setSections(sectionDTOs);

        return dto;
    }

}