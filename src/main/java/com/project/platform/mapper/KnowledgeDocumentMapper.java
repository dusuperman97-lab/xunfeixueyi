package com.project.platform.mapper;

import com.project.platform.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper {
    
    @Insert("INSERT INTO knowledge_document(kb_id, student_id, file_name, file_path, file_size, file_type, " +
            "content_summary, segment_count, embedding_status, status) " +
            "VALUES(#{kbId}, #{studentId}, #{fileName}, #{filePath}, #{fileSize}, #{fileType}, " +
            "#{contentSummary}, #{segmentCount}, #{embeddingStatus}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeDocument document);
    
    @Update("UPDATE knowledge_document SET file_name=#{fileName}, file_path=#{filePath}, file_size=#{fileSize}, " +
            "file_type=#{fileType}, content_summary=#{contentSummary}, segment_count=#{segmentCount}, " +
            "embedding_status=#{embeddingStatus}, updated_at=NOW() WHERE id=#{id}")
    int update(KnowledgeDocument document);
    
    @Update("UPDATE knowledge_document SET status=0 WHERE id=#{id}")
    int deleteById(Long id);
    
    @Select("SELECT * FROM knowledge_document WHERE id=#{id} AND status=1")
    KnowledgeDocument selectById(Long id);
    
    @Select("SELECT * FROM knowledge_document WHERE kb_id=#{kbId} AND status=1 ORDER BY created_at DESC")
    List<KnowledgeDocument> selectByKbId(Long kbId);
    
    @Select("SELECT * FROM knowledge_document WHERE student_id=#{studentId} AND status=1 ORDER BY created_at DESC")
    List<KnowledgeDocument> selectByStudentId(Long studentId);
    
    @Select("SELECT COUNT(*) FROM knowledge_document WHERE kb_id=#{kbId} AND status=1 AND embedding_status=2")
    int countByKbId(Long kbId);
}
