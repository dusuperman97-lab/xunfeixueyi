package com.project.platform.service;

import com.project.platform.dto.LearningPathDTO;
import com.project.platform.entity.LearningPath;
import com.project.platform.mapper.LearningPathMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 学习路线Service
 */
@Service
@Slf4j
public class LearningPathService {

    @Autowired
    private LearningPathMapper learningPathMapper;

    /**
     * 保存或更新学习路线
     */
    public LearningPath saveOrUpdate(LearningPath learningPath) {
        if (learningPath.getId() == null) {
            learningPathMapper.insert(learningPath);
            log.info("=== 新增学习路线成功，ID: {}", learningPath.getId());
        } else {
            learningPathMapper.updateById(learningPath);
            log.info("=== 更新学习路线成功，ID: {}", learningPath.getId());
        }
        return learningPath;
    }

    /**
     * 根据用户ID和类型查询最新路线
     */
    public LearningPath getLatestByUserIdAndType(Integer userId, String pathType) {
        return learningPathMapper.selectByUserIdAndType(userId, pathType);
    }

    /**
     * 查询用户所有学习路线
     */
    public List<LearningPath> getAllByUserId(Integer userId) {
        return learningPathMapper.selectByUserId(userId);
    }

    /**
     * 删除学习路线
     */
    public void deleteById(Integer id) {
        learningPathMapper.deleteById(id);
        log.info("=== 删除学习路线成功，ID: {}", id);
    }
}
