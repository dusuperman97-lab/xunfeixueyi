package com.project.platform.mapper;

import com.project.platform.entity.Section;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 章节小节 Mapper 接口
 */
public interface SectionMapper {

    List<Section> queryPage(@Param("offset") Integer offset,
                            @Param("pageSize") Integer pageSize,
                            @Param("query") Map<String, Object> query);

    int queryCount(@Param("query") Map<String, Object> query);

    Section selectById(Integer id);

    List<Section> listByChapterId(Integer chapterId);

    /**
     * 批量查询：根据章节ID列表一次性查出所有小节
     */
    List<Section> listByChapterIds(@Param("chapterIds") List<Integer> chapterIds);

    Section selectByChapterIdAndName(@Param("chapterId") Integer chapterId,
                                     @Param("sectionName") String sectionName);

    void insert(Section entity);

    void updateById(Section entity);

    void removeByIds(List<Integer> ids);

    void removeByChapterId(Integer chapterId);  // 级联删除用
}