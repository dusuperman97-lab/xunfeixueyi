package com.project.platform.mapper;

import com.project.platform.entity.QaHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 知识库问答历史 Mapper
 */
@Mapper
public interface QaHistoryMapper {

    @Insert("INSERT INTO knowledge_qa_history(student_id, kb_id, original_question, rewritten_question, " +
            "actual_search_query, answer, sources_json, citations_json, " +
            "feedback, feedback_comment, retrieval_time, total_time, result_count, used_knowledge) " +
            "VALUES(#{studentId}, #{kbId}, #{originalQuestion}, #{rewrittenQuestion}, " +
            "#{actualSearchQuery}, #{answer}, #{sourcesJson}, #{citationsJson}, " +
            "#{feedback}, #{feedbackComment}, #{retrievalTime}, #{totalTime}, #{resultCount}, #{usedKnowledge})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QaHistory history);

    @Select("SELECT * FROM knowledge_qa_history WHERE id=#{id}")
    QaHistory selectById(Long id);

    @Select("SELECT * FROM knowledge_qa_history WHERE student_id=#{studentId} " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<QaHistory> selectByStudentId(@Param("studentId") Long studentId,
                                       @Param("limit") int limit,
                                       @Param("offset") int offset);

    @Select("SELECT * FROM knowledge_qa_history WHERE student_id=#{studentId} AND kb_id=#{kbId} " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<QaHistory> selectByStudentAndKb(@Param("studentId") Long studentId,
                                          @Param("kbId") Long kbId,
                                          @Param("limit") int limit,
                                          @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM knowledge_qa_history WHERE student_id=#{studentId}")
    int countByStudentId(Long studentId);

    @Select("SELECT COUNT(*) FROM knowledge_qa_history WHERE student_id=#{studentId} AND kb_id=#{kbId}")
    int countByStudentAndKb(@Param("studentId") Long studentId, @Param("kbId") Long kbId);

    @Update("UPDATE knowledge_qa_history SET feedback=#{feedback}, feedback_comment=#{feedbackComment} " +
            "WHERE id=#{id} AND student_id=#{studentId}")
    int updateFeedback(@Param("id") Long id,
                       @Param("studentId") Long studentId,
                       @Param("feedback") Integer feedback,
                       @Param("feedbackComment") String feedbackComment);

    @Delete("DELETE FROM knowledge_qa_history WHERE id=#{id} AND student_id=#{studentId}")
    int deleteByIdAndStudent(@Param("id") Long id, @Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM knowledge_qa_history WHERE student_id=#{studentId} " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int countByStudentIdAndDays(@Param("studentId") Long studentId, @Param("days") int days);

    @Select("SELECT feedback, COUNT(*) AS cnt FROM knowledge_qa_history " +
            "WHERE student_id=#{studentId} GROUP BY feedback")
    List<Map<String, Object>> selectFeedbackSummary(@Param("studentId") Long studentId);

    @Select("SELECT feedback, COUNT(*) AS cnt FROM knowledge_qa_history " +
            "WHERE student_id=#{studentId} " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "GROUP BY feedback")
    List<Map<String, Object>> selectFeedbackSummaryByDays(@Param("studentId") Long studentId, @Param("days") int days);

    @Select("SELECT HOUR(created_at) AS hour, COUNT(*) AS cnt " +
            "FROM knowledge_qa_history WHERE student_id=#{studentId} " +
            "GROUP BY HOUR(created_at) ORDER BY hour")
    List<Map<String, Object>> selectHourDistribution(@Param("studentId") Long studentId);

    @Select("SELECT HOUR(created_at) AS hour, COUNT(*) AS cnt " +
            "FROM knowledge_qa_history WHERE student_id=#{studentId} " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "GROUP BY HOUR(created_at) ORDER BY hour")
    List<Map<String, Object>> selectHourDistributionByDays(@Param("studentId") Long studentId, @Param("days") int days);
}
