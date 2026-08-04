package com.project.platform.service;

import com.project.platform.entity.StudentProfile;
import com.project.platform.vo.StudentProfileDashboardVO;

/**
 * 学生画像服务接口
 */
public interface StudentProfileService {

    /**
     * 根据学生ID获取画像
     * @param studentId 学生ID
     * @return 学生画像，如果不存在返回null
     */
    StudentProfile getProfileByStudentId(Integer studentId);

    /**
     * 获取数据大屏仪表盘数据
     * @param studentId 学生ID
     * @return 大屏综合数据，如果画像不存在返回默认空结构
     */
    StudentProfileDashboardVO getDashboard(Integer studentId);

    /**
     * 保存或更新学生画像（增量更新）
     * @param profile 画像数据
     * @return 保存后的画像
     */
    StudentProfile saveOrUpdateProfile(StudentProfile profile);

    /**
     * 删除学生画像
     * @param studentId 学生ID
     */
    void deleteProfile(Integer studentId);
}
