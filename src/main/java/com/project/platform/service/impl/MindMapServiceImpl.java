package com.project.platform.service.impl;

import com.project.platform.entity.MindMap;
import com.project.platform.mapper.MindMapMapper;
import com.project.platform.service.MindMapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 思维导图服务实现类
 */
@Slf4j
@Service
public class MindMapServiceImpl implements MindMapService {

    @Autowired
    private MindMapMapper mindMapMapper;

    @Override
    public MindMap saveMindMap(MindMap mindMap) {
        log.info("=== 保存思维导图记录，标题: {}", mindMap.getTitle());
        mindMapMapper.insert(mindMap);
        log.info("=== 思维导图记录保存成功，ID: {}", mindMap.getId());
        return mindMap;
    }

    @Override
    public MindMap getMindMapById(Long id) {
        log.info("=== 查询思维导图，ID: {}", id);
        return mindMapMapper.selectById(id);
    }

    @Override
    public List<MindMap> getUserMindMaps(Integer userId) {
        log.info("=== 查询用户思维导图列表，userId: {}", userId);
        return mindMapMapper.selectByUserId(userId);
    }

    @Override
    public List<MindMap> getMindMapsByChapterId(Integer chapterId) {
        log.info("=== 根据章节ID查询思维导图，chapterId: {}", chapterId);
        return mindMapMapper.selectByChapterId(chapterId);
    }

    @Override
    public List<MindMap> getMindMapsBySectionId(Integer sectionId) {
        log.info("=== 根据小节ID查询思维导图，sectionId: {}", sectionId);
        return mindMapMapper.selectBySectionId(sectionId);
    }

    @Override
    public boolean updateTitle(Long id, String title) {
        log.info("=== 更新思维导图标题，ID: {}, 新标题: {}", id, title);
        int result = mindMapMapper.updateTitle(id, title);
        return result > 0;
    }

    @Override
    public boolean deleteMindMap(Long id) {
        log.info("=== 删除思维导图，ID: {}", id);
        int result = mindMapMapper.deleteById(id);
        return result > 0;
    }

    @Override
    public int deleteBatch(String ids) {
        log.info("=== 批量删除思维导图，IDs: {}", ids);
        return mindMapMapper.deleteBatch(ids);
    }
}
