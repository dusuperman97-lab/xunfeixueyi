package com.project.platform.rag.store;

import com.project.platform.agents.config.utils.RagTools;
import com.project.platform.entity.KnowledgeBase;
import com.project.platform.entity.KnowledgeDocument;
import com.project.platform.mapper.KnowledgeBaseMapper;
import com.project.platform.mapper.KnowledgeDocumentMapper;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pinecone 向量清理服务
 * <p>
 * 当知识库或文档被删除时，同步清理 Pinecone 中对应的向量数据，
 * 防止存储泄漏和数据泄漏。
 * <p>
 * 使用 Pinecone REST API 的 /vectors/delete 端点，通过 metadata filter
 * 批量删除匹配的向量。
 */
@Slf4j
@Service
public class VectorCleanupService {

    private static final String PINECONE_CONTROLLER_HOST = "https://xunfei-index-external.pinecone.io";

    private final RestTemplate restTemplate;

    private final String apiKey;

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 15000;

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private RagTools ragTools;

    public VectorCleanupService(@Value("${pinecone_key}") String apiKey) {
        this.apiKey = apiKey;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 删除指定文档在 Pinecone 中的所有向量分片
     *
     * @param docId 文档ID
     * @param namespace Pinecone namespace
     */
    public void deleteVectorsByDocId(Long docId, String namespace) {
        if (docId == null || namespace == null || namespace.isBlank()) {
            log.warn("deleteVectorsByDocId 参数无效: docId={}, namespace={}", docId, namespace);
            return;
        }
        deleteByMetadataFilter("doc_id", docId.toString(), namespace);
    }

    /**
     * 删除指定知识库在 Pinecone 中的所有向量分片
     *
     * @param kbId 知识库ID
     * @param namespace Pinecone namespace
     */
    public void deleteVectorsByKbId(Long kbId, String namespace) {
        if (kbId == null || namespace == null || namespace.isBlank()) {
            log.warn("deleteVectorsByKbId 参数无效: kbId={}, namespace={}", kbId, namespace);
            return;
        }
        deleteByMetadataFilter("kb_id", kbId.toString(), namespace);
    }

    /**
     * 删除指定 studentId 在特定 namespace 中的向量
     *
     * @param studentId 用户ID
     * @param namespace Pinecone namespace
     */
    public void deleteVectorsByStudentId(Long studentId, String namespace) {
        if (studentId == null || namespace == null || namespace.isBlank()) {
            log.warn("deleteVectorsByStudentId 参数无效: studentId={}, namespace={}", studentId, namespace);
            return;
        }
        deleteByMetadataFilter("student_id", studentId.toString(), namespace);
    }

    /**
     * 通过 metadata filter 删除向量
     * <p>
     * 调用 Pinecone REST API: POST /vectors/delete
     * 请求体: { "filter": { "metadata_field": { "$eq": "value" } }, "namespace": "xxx" }
     *
     * @param field  metadata 字段名
     * @param value  metadata 字段值
     * @param namespace Pinecone namespace
     */
    private void deleteByMetadataFilter(String field, String value, String namespace) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Api-Key", apiKey);

            // 构建 Pinecone delete 请求体
            Map<String, Object> body = new LinkedHashMap<>();
            // metadata filter: { "field": { "$eq": "value" } }
            Map<String, Object> eqCondition = new LinkedHashMap<>();
            eqCondition.put("$eq", value);
            Map<String, Object> filter = new LinkedHashMap<>();
            filter.put(field, eqCondition);
            body.put("filter", filter);
            body.put("namespace", namespace);

            String url = PINECONE_CONTROLLER_HOST + "/vectors/delete";

            log.info("开始清理 Pinecone 向量: field={}, value={}, namespace={}", field, value, namespace);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Pinecone 向量清理成功: field={}, value={}, namespace={}, status={}",
                        field, value, namespace, response.getStatusCode());
            } else {
                log.warn("Pinecone 向量清理返回非 2xx: field={}, value={}, namespace={}, status={}",
                        field, value, namespace, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Pinecone 向量清理失败: field={}, value={}, namespace={}, error={}",
                    field, value, namespace, e.getMessage());
            // 不抛异常，避免阻塞主流程（向量数据残留优于业务中断）
        }
    }

    /**
     * 删除指定文档中指定 chunk_index 的分片向量
     *
     * @param docId     文档ID
     * @param namespace  Pinecone namespace
     * @param chunkIndex 分片索引
     */
    public void deleteByMetadataAndChunkIndex(Long docId, String namespace, int chunkIndex) {
        if (docId == null || namespace == null || namespace.isBlank()) {
            log.warn("deleteByMetadataAndChunkIndex 参数无效: docId={}, namespace={}, chunkIndex={}",
                    docId, namespace, chunkIndex);
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Api-Key", apiKey);

            // 用 Pinecone 的 $and filter 匹配多个 metadata 条件
            List<Map<String, Object>> andConditions = new ArrayList<>();

            Map<String, Object> docIdCondition = new LinkedHashMap<>();
            docIdCondition.put("doc_id", Map.of("$eq", docId.toString()));
            andConditions.add(docIdCondition);

            Map<String, Object> chunkIndexCondition = new LinkedHashMap<>();
            chunkIndexCondition.put("chunk_index", Map.of("$eq", String.valueOf(chunkIndex)));
            andConditions.add(chunkIndexCondition);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("filter", Map.of("$and", andConditions));
            body.put("namespace", namespace);

            String url = PINECONE_CONTROLLER_HOST + "/vectors/delete";

            log.info("按分片清理 Pinecone 向量: docId={}, chunkIndex={}, namespace={}", docId, chunkIndex, namespace);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Pinecone 分片向量清理成功: docId={}, chunkIndex={}, namespace={}",
                        docId, chunkIndex, namespace);
            } else {
                log.warn("Pinecone 分片向量清理返回非 2xx: status={}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Pinecone 分片向量清理失败: docId={}, chunkIndex={}, namespace={}, error={}",
                    docId, chunkIndex, namespace, e.getMessage());
        }
    }

    /**
     * 删除整个 namespace 的所有向量
     * 用于知识库被删除时彻底清理
     *
     * @param namespace Pinecone namespace
     */
    public void deleteAllInNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            log.warn("deleteAllInNamespace 参数无效: namespace={}", namespace);
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Api-Key", apiKey);

            // 删除 namespace 下所有向量：不传 filter，只传 namespace
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("namespace", namespace);
            // deleteAll 需要传一个 match-all filter
            body.put("deleteAll", true);

            String url = PINECONE_CONTROLLER_HOST + "/vectors/delete";

            log.info("开始清空 namespace 所有向量: namespace={}", namespace);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Pinecone namespace 清理成功: namespace={}", namespace);
            } else {
                log.warn("Pinecone namespace 清理返回非 2xx: namespace={}, status={}",
                        namespace, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Pinecone namespace 清理失败: namespace={}, error={}",
                    namespace, e.getMessage());
        }
    }
}
