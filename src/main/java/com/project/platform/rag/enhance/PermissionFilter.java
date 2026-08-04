package com.project.platform.rag.enhance;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限过滤器
 * <p>
 * 根据用户身份对检索结果做二次过滤，确保用户只能看到有权限的内容。
 * <p>
 * 为什么需要权限过滤？
 * 向量检索是按语义相似度返回结果的，不会考虑权限。
 * 在多租户场景下，不做过滤可能导致用户看到别人的数据。
 */
@Slf4j
@Component
public class PermissionFilter {

    public List<Content> filterByStudentId(List<Content> contents, Long studentId) {
        if (contents == null || contents.isEmpty()) {
            return contents;
        }
        // studentId为空时不进行过滤，返回全部结果
        if (studentId == null) {
            log.warn("studentId为null，跳过权限过滤");
            return contents;
        }

        String studentIdStr = studentId.toString();

        List<Content> filtered = contents.stream()
                .filter(content -> {
                    TextSegment segment = content.textSegment();
                    if (segment == null || segment.metadata() == null) {
                        // 无元数据的内容默认拒绝，防止多租户数据泄露
                        log.debug("权限过滤：内容缺少元数据，已拒绝");
                        return false;
                    }
                    String contentStudentId = segment.metadata().getString("student_id");
                    if (contentStudentId == null) {
                        // 全局知识库内容无student_id标记，默认放行（公共资源）
                        return true;
                    }
                    return studentIdStr.equals(contentStudentId);
                })
                .collect(Collectors.toList());

        if (filtered.size() < contents.size()) {
            log.info("权限过滤: 原始{}条 -> 过滤后{}条 (studentId={})",
                    contents.size(), filtered.size(), studentId);
        }

        return filtered;
    }

    public List<Content> filterByKbId(List<Content> contents, Long kbId) {
        if (contents == null || contents.isEmpty() || kbId == null) {
            return contents;
        }

        String kbIdStr = kbId.toString();

        return contents.stream()
                .filter(content -> {
                    TextSegment segment = content.textSegment();
                    if (segment == null || segment.metadata() == null) {
                        return true;
                    }
                    String contentKbId = segment.metadata().getString("kb_id");
                    if (contentKbId == null) {
                        return true;
                    }
                    return kbIdStr.equals(contentKbId);
                })
                .collect(Collectors.toList());
    }

    public List<Content> filter(List<Content> contents, Long studentId, Long kbId) {
        List<Content> result = filterByStudentId(contents, studentId);
        if (kbId != null) {
            result = filterByKbId(result, kbId);
        }
        return result;
    }
}
