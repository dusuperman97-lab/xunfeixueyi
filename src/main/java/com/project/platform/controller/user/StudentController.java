package com.project.platform.controller.user;

import com.project.platform.entity.Student;
import com.project.platform.service.StudentService;
import com.project.platform.vo.PageVO;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生管理
 */
@RestController
@RequestMapping("/student")
public class StudentController {

    @Resource
    private StudentService studentService;

    /**
     * 分页查询学生列表
     *
     * @param query    动态查询条件 (如 username, studentNo 等)
     * @param pageNum  当前页码
     * @param pageSize 每页条数
     * @return
     */
    @GetMapping("/page")
    public ResponseVO<PageVO<Student>> page(@RequestParam Map<String, Object> query,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 从查询参数中移除分页参数，防止影响底层的 SQL 动态条件拼接
        query.remove("pageNum");
        query.remove("pageSize");

        PageVO<Student> pageVO = studentService.page(query, pageNum, pageSize);
        return ResponseVO.ok(pageVO);
    }

    /**
     * 查询所有学生列表 (不分页)
     *
     * @return
     */
    @GetMapping("/list")
    public ResponseVO<List<Student>> list() {
        return ResponseVO.ok(studentService.list());
    }

    /**
     * 根据ID获取学生详情
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public ResponseVO<Student> getById(@PathVariable Integer id) {
        return ResponseVO.ok(studentService.selectById(id));
    }

    /**
     * 新增学生
     *
     * @param student
     * @return
     */
    @PostMapping("/add")
    public ResponseVO<Void> add(@RequestBody Student student) {
        studentService.insert(student);
        return ResponseVO.ok();
    }

    /**
     * 修改学生信息
     *
     * @param student
     * @return
     */
    @PutMapping("/update")
    public ResponseVO<Void> update(@RequestBody Student student) {
        studentService.updateById(student);
        return ResponseVO.ok();
    }

    /**
     * 批量删除学生
     *
     * @param ids 接收 JSON 数组格式的 ID 列表，例如：[1, 2, 3]
     * @return
     */
    @PostMapping("/delete")
    public ResponseVO<Void> delete(@RequestBody List<Integer> ids) {
        if (ids != null && !ids.isEmpty()) {
            studentService.removeByIds(ids);
        }
        return ResponseVO.ok();
    }
}
