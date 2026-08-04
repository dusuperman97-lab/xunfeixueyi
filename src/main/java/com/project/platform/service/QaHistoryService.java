package com.project.platform.service;

import com.project.platform.entity.QaHistory;

import java.util.List;

/**
 * 知识库问答历史服务接口
 */
public interface QaHistoryService {

    /**
     * 保存问答记录
     */
    QaHistory saveHistory(QaHistory history);

    /**
     * 分页查询用户的问答历史
     */
    List<QaHistory> getHistoryByStudent(Long studentId, int page, int pageSize);

    /**
     * 分页查询用户在指定知识库的问答历史
     */
    List<QaHistory> getHistoryByStudentAndKb(Long studentId, Long kbId, int page, int pageSize);

    /**
     * 查询问答历史总数
     */
    int countByStudent(Long studentId);

    /**
     * 查询指定知识库的问答历史总数
     */
    int countByStudentAndKb(Long studentId, Long kbId);

    /**
     * 提交答案反馈
     */
    void submitFeedback(Long historyId, Long studentId, Integer feedback, String comment);

    /**
     * 删除问答记录
     */
    void deleteHistory(Long historyId, Long studentId);
}
