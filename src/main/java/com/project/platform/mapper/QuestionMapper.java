package com.project.platform.mapper;

import com.project.platform.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 随堂练习题Mapper接口
 */
@Mapper
public interface QuestionMapper {

    /**
     * 根据小节ID查询所有题目
     */
    List<Question> selectBySectionId(@Param("sectionId") Integer sectionId);

    /**
     * 批量查询：根据小节ID列表一次性查出所有题目
     */
    List<Question> listBySectionIds(@Param("sectionIds") List<Integer> sectionIds);

    /**
     * 根据章节ID查询所有题目
     */
    List<Question> selectByCourseId(@Param("courseId") Integer courseId);

    /**
     * 批量插入题目
     */
    int batchInsert(@Param("questions") List<Question> questions);

    /**
     * 批量更新题目
     */
    int batchUpdateBySectionId(@Param("questions") List<Question> questions);

    /**
     * 根据小节ID删除题目
     */
    int deleteBySectionId(@Param("sectionId") Integer sectionId);

    /**
     * 分页查询课程下的测试题列表（限制条数）
     */
    List<Question> queryCourseQuestionsPage(@Param("offset") int offset,
                                            @Param("pageSize") int pageSize,
                                            @Param("courseId") Integer courseId,
                                            @Param("questionType") String questionType);

    /**
     * 查询课程下的测试题总记录数（用于计算分页）
     */
    long queryCourseQuestionsCount(@Param("courseId") Integer courseId,
                                   @Param("questionType") String questionType);

    /**
     * 根据课程ID和小节ID查询题目，可选按测试批次号过滤
     */
    List<Question> selectByCourseAndSectionId(@Param("courseId") Integer courseId,
                                              @Param("sectionId") Integer sectionId,
                                              @Param("testBatchNo") String testBatchNo);

    /**
     * 根据课程ID和题目类别查询题目（用于课程考试）
     * @param testBatchNo 可选，按测试批次号过滤
     */
    List<Question> selectByCourseIdAndCategory(@Param("courseId") Integer courseId,
                                                @Param("questionCategory") String questionCategory,
                                                @Param("testBatchNo") String testBatchNo);

    /**
     * 根据题目ID查询题目详情
     */
    Question selectById(@Param("id") Integer id);

    /**
     * 根据测试批次号查询题目
     */
    List<Question> selectByBatchNo(@Param("testBatchNo") String testBatchNo);

    /**
     * 根据课程ID和题目类别查询最新的 generation batch 题目
     * @param sectionId 可选，用于小节级别的批次隔离（SECTION_PRACTICE 场景）
     */
    List<Question> selectLatestGenBatchByCourseAndCategory(@Param("courseId") Integer courseId,
                                                            @Param("questionCategory") String questionCategory,
                                                            @Param("sectionId") Integer sectionId);

    /**
     * 查询所有题目总数
     */
    long queryAllCount();

    /**
     * 根据测试批次号更新题目（将旧 batchNo 的题目迁移到新 batchNo）
     */
    int updateTestBatchNo(@Param("oldBatchNo") String oldBatchNo,
                          @Param("newBatchNo") String newBatchNo);

    /**
     * 根据题目ID列表批量更新测试批次号（用于回退路径的题目迁移）
     */
    int updateTestBatchNoByIds(@Param("ids") List<Integer> ids,
                               @Param("newBatchNo") String newBatchNo);
}
