package com.project.platform.mapper;

import com.project.platform.entity.KnowledgeBase;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeBaseMapper {
    
    @Insert("INSERT INTO knowledge_base(student_id, name, description, document_count, status, pinecone_namespace) " +
            "VALUES(#{studentId}, #{name}, #{description}, #{documentCount}, #{status}, #{pineconeNamespace})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeBase knowledgeBase);
    
    @Update("UPDATE knowledge_base SET name=#{name}, description=#{description}, document_count=#{documentCount}, " +
            "status=#{status}, pinecone_namespace=#{pineconeNamespace}, updated_at=NOW() WHERE id=#{id}")
    int update(KnowledgeBase knowledgeBase);
    
    @Delete("DELETE FROM knowledge_base WHERE id=#{id}")
    int deleteById(Long id);
    
    @Select("SELECT * FROM knowledge_base WHERE id=#{id}")
    KnowledgeBase selectById(Long id);
    
    @Select("SELECT * FROM knowledge_base WHERE student_id=#{studentId} AND status=1 ORDER BY created_at DESC")
    List<KnowledgeBase> selectByStudentId(Long studentId);
    
    @Select("SELECT * FROM knowledge_base WHERE student_id=#{studentId} AND pinecone_namespace=#{namespace} AND status=1")
    KnowledgeBase selectByNamespace(Long studentId, String namespace);
}
