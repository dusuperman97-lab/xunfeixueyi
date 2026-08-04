package com.project.platform.mapper;

import com.project.platform.entity.MindMap;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 思维导图Mapper接口
 */
@Mapper
public interface MindMapMapper {

    /**
     * 插入思维导图记录
     */
    @Insert("INSERT INTO mind_map (user_id, title, file_url, chapter_id, section_id, source_type, topic) " +
            "VALUES (#{userId}, #{title}, #{fileUrl}, #{chapterId}, #{sectionId}, #{sourceType}, #{topic})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MindMap mindMap);

    /**
     * 根据ID查询思维导图
     */
    @Select("SELECT * FROM mind_map WHERE id = #{id}")
    MindMap selectById(Long id);

    /**
     * 根据用户ID查询所有思维导图（按创建时间倒序）
     */
    @Select("SELECT * FROM mind_map WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<MindMap> selectByUserId(Integer userId);

    /**
     * 根据章节ID查询思维导图
     */
    @Select("SELECT * FROM mind_map WHERE chapter_id = #{chapterId} ORDER BY create_time DESC")
    List<MindMap> selectByChapterId(Integer chapterId);

    /**
     * 根据小节ID查询思维导图
     */
    @Select("SELECT * FROM mind_map WHERE section_id = #{sectionId} ORDER BY create_time DESC")
    List<MindMap> selectBySectionId(Integer sectionId);

    /**
     * 更新思维导图标题
     */
    @Update("UPDATE mind_map SET title = #{title} WHERE id = #{id}")
    int updateTitle(@Param("id") Long id, @Param("title") String title);

    /**
     * 删除思维导图
     */
    @Delete("DELETE FROM mind_map WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 批量删除思维导图
     */
    @Delete("DELETE FROM mind_map WHERE id IN (${ids})")
    int deleteBatch(@Param("ids") String ids);

    /**
     * 根据用户ID和章节/小节ID删除
     */
    @Delete("DELETE FROM mind_map WHERE user_id = #{userId} AND " +
            "(chapter_id = #{chapterId} OR section_id = #{sectionId})")
    int deleteByUserAndCourse(@Param("userId") Integer userId, 
                              @Param("chapterId") Integer chapterId, 
                              @Param("sectionId") Integer sectionId);
}
