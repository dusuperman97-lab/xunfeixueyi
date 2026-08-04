package com.project.platform.service.impl;

import com.project.platform.entity.Section;
import com.project.platform.exception.CustomException;
import com.project.platform.mapper.SectionMapper;
import com.project.platform.service.SectionService;
import com.project.platform.vo.PageVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 章节小节服务实现类
 */
@Service
public class SectionServiceImpl implements SectionService {
    @Resource
    private SectionMapper sectionMapper;

    @Override
    public void learnComplete(Integer userId, Integer sectionId) {
        Section section1 = sectionMapper.selectById(sectionId);
        if(section1.getIsCompleted()!= null && section1.getIsCompleted() == true)
        {
            return;
        }
        Section section = new Section();
        section.setId(sectionId);
        section.setIsCompleted(true);
        sectionMapper.updateById(section);
    }

    @Override
    public PageVO<Section> page(Map<String, Object> query, Integer pageNum, Integer pageSize) {
        PageVO<Section> page = new PageVO();
        List<Section> list = sectionMapper.queryPage((pageNum - 1) * pageSize, pageSize, query);
        page.setList(list);
        page.setTotal(sectionMapper.queryCount(query));
        return page;
    }

    @Override
    public Section selectById(Integer id) {
        return sectionMapper.selectById(id);
    }

    @Override
    public List<Section> listByChapterId(Integer chapterId) {
        return sectionMapper.listByChapterId(chapterId);
    }

    @Override
    public void insert(Section entity) {
        check(entity);
        sectionMapper.insert(entity);
    }

    @Override
    public void updateById(Section entity) {
        sectionMapper.updateById(entity);
    }

    private void check(Section entity) {
        if (entity.getChapterId() == null) {
            throw new CustomException("请选择所属章节");
        }
        // 校验同章节下小节名称不能重复
        Section section = sectionMapper.selectByChapterIdAndName(entity.getChapterId(), entity.getSectionName());
        if (section != null && !section.getId().equals(entity.getId())) {
            throw new CustomException("该章节下已存在相同名称的小节");
        }
    }

    @Override
    public void removeByIds(List<Integer> ids) {
        sectionMapper.removeByIds(ids);
    }
}
