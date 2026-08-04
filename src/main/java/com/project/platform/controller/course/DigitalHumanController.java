package com.project.platform.controller.course;

import com.project.platform.agents.ai.AiCourseHelperService;
import com.project.platform.agents.ai.KnowledgeRagHelperService;
import com.project.platform.annotation.TrackAiUsage;
import com.project.platform.entity.AiToolUsage;
import com.project.platform.entity.DigitalHuman;
import com.project.platform.service.DigitalHumanService;
import com.project.platform.vo.PageVO;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 数字人
 */
@RestController
@RequestMapping("/digitalHuman")
@Slf4j
public class DigitalHumanController {
    @Resource
    private DigitalHumanService digitalHumanService;

    @Resource
    private AiCourseHelperService aiCourseHelperService;

    @Resource
    private KnowledgeRagHelperService knowledgeRagHelperService;

    /**
     * 分页查询
     *
     * @param query
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("page")
    public ResponseVO<PageVO<DigitalHuman>> page(@RequestParam Map<String, Object> query, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        PageVO<DigitalHuman> page = digitalHumanService.page(query, pageNum, pageSize);
        return ResponseVO.ok(page);
    }

    /**
     * 根据id查询
     *
     * @param id
     * @return
     */
    @GetMapping("selectById/{id}")
    public ResponseVO<DigitalHuman> selectById(@PathVariable("id") Integer id) {
        DigitalHuman entity = digitalHumanService.selectById(id);
        return ResponseVO.ok(entity);
    }

    /**
     * 列表
     *
     * @return
     */
    @GetMapping("list")
    public ResponseVO<List<DigitalHuman>> list() {
        return ResponseVO.ok(digitalHumanService.list());
    }

    /**
     * 新增
     *
     * @param entity
     * @return
     */
    @PostMapping("add")
    public ResponseVO add(@RequestBody DigitalHuman entity) {
        digitalHumanService.insert(entity);
        return ResponseVO.ok();
    }

    /**
     * 更新
     *
     * @param entity
     * @return
     */
    @PutMapping("update")
    public ResponseVO update(@RequestBody DigitalHuman entity) {
        digitalHumanService.updateById(entity);
        return ResponseVO.ok();
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping("delBatch")
    public ResponseVO delBatch(@RequestBody List<Integer> ids) {
        digitalHumanService.removeByIds(ids);
        return ResponseVO.ok();
    }

    /**
     * 数字人流式对话
     * 根据数字人ID查询名称，动态生成角色提示词，与用户问题拼接后调用大模型流式对话
     *
     * @param body 请求体，包含 id(Integer) 和 message(String)
     * @return SSE 流式对话结果
     */
    @PostMapping(value = "streamingChat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @TrackAiUsage(AiToolUsage.ToolType.VIRTUAL_TEACHER)
    public SseEmitter streamingChat(@RequestBody Map<String, Object> body) {
        SseEmitter emitter = new SseEmitter(300000L);

        Integer id = body.get("id") != null ? Integer.valueOf(body.get("id").toString()) : null;
        String message = body.get("message") != null ? body.get("message").toString() : "";

        if (id == null) {
            emitter.completeWithError(new IllegalArgumentException("id 不能为空"));
            return emitter;
        }

        // 1. 根据数字人ID查询数字人名称
        DigitalHuman digitalHuman = digitalHumanService.selectById(id);
        String digitalHumanName = digitalHuman.getName();
        log.info("=== 数字人流式对话，id: {}, name: {}, messageLen: {}", id, digitalHumanName, message.length());

        // 2. 动态生成角色提示词
        String rolePrompt = "你现在是" + digitalHumanName + "，请以" + digitalHumanName + "的身份和口吻来回答用户的问题。";

        // 3. 将角色提示词与用户问题进行拼接
        String combinedMessage = rolePrompt + "\n" + message;

        // 4. 调用大模型流式对话，逐token写入SSE
        Flux<String> streamFlux = knowledgeRagHelperService.streamChat(combinedMessage);
        streamFlux.subscribe(
                data -> {
                    try {
                        emitter.send(SseEmitter.event().data(data));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.error("数字人流式对话异常", error);
                    emitter.completeWithError(error);
                },
                () -> emitter.complete()
        );

        return emitter;
    }

}
