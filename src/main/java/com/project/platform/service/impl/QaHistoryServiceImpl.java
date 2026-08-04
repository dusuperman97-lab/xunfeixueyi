package com.project.platform.service.impl;

import com.project.platform.entity.QaHistory;
import com.project.platform.mapper.QaHistoryMapper;
import com.project.platform.service.QaHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class QaHistoryServiceImpl implements QaHistoryService {

    @Resource
    private QaHistoryMapper qaHistoryMapper;

    @Override
    public QaHistory saveHistory(QaHistory history) {
        if (history == null) {
            log.warn("saveHistory 收到空记录，跳过");
            return null;
        }
        int rows = qaHistoryMapper.insert(history);
        if (rows > 0) {
            log.info("问答历史保存成功: id={}, question={}", history.getId(), history.getOriginalQuestion());
            return history;
        }
        log.warn("问答历史保存失败: question={}", history.getOriginalQuestion());
        return null;
    }

    @Override
    public List<QaHistory> getHistoryByStudent(Long studentId, int page, int pageSize) {
        if (studentId == null) {
            return Collections.emptyList();
        }
        int offset = Math.max(0, page - 1) * pageSize;
        return qaHistoryMapper.selectByStudentId(studentId, pageSize, offset);
    }

    @Override
    public List<QaHistory> getHistoryByStudentAndKb(Long studentId, Long kbId, int page, int pageSize) {
        if (studentId == null || kbId == null) {
            return Collections.emptyList();
        }
        int offset = Math.max(0, page - 1) * pageSize;
        return qaHistoryMapper.selectByStudentAndKb(studentId, kbId, pageSize, offset);
    }

    @Override
    public int countByStudent(Long studentId) {
        if (studentId == null) {
            return 0;
        }
        return qaHistoryMapper.countByStudentId(studentId);
    }

    @Override
    public int countByStudentAndKb(Long studentId, Long kbId) {
        if (studentId == null || kbId == null) {
            return 0;
        }
        return qaHistoryMapper.countByStudentAndKb(studentId, kbId);
    }

    @Override
    public void submitFeedback(Long historyId, Long studentId, Integer feedback, String comment) {
        if (historyId == null || studentId == null) {
            log.warn("submitFeedback 参数无效: historyId={}, studentId={}", historyId, studentId);
            return;
        }
        int rows = qaHistoryMapper.updateFeedback(historyId, studentId, feedback, comment);
        if (rows > 0) {
            log.info("问答反馈提交成功: historyId={}, feedback={}", historyId, feedback);
        } else {
            log.warn("问答反馈提交失败（记录不存在或无权操作）: historyId={}, studentId={}", historyId, studentId);
        }
    }

    @Override
    public void deleteHistory(Long historyId, Long studentId) {
        if (historyId == null || studentId == null) {
            log.warn("deleteHistory 参数无效: historyId={}, studentId={}", historyId, studentId);
            return;
        }
        int rows = qaHistoryMapper.deleteByIdAndStudent(historyId, studentId);
        if (rows > 0) {
            log.info("问答历史删除成功: historyId={}", historyId);
        } else {
            log.warn("问答历史删除失败（记录不存在或无权操作）: historyId={}, studentId={}", historyId, studentId);
        }
    }
}
