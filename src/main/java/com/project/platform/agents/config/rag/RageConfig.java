package com.project.platform.agents.config.rag;

import com.project.platform.agents.config.utils.RagTools;
import com.project.platform.rag.ingest.DocumentCleanHandler;
import com.project.platform.rag.ingest.SmartDocumentSplitter;
import com.project.platform.service.KnowledgeDocumentService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * RAG 配置类
 * 提供文档处理、向量化和检索的基础配置
 * <p>
 * 增强版文档处理流程：
 * 1. 文档加载
 * 2. 文档清洗（去除冗余空白、重复段落）
 * 3. 智能分片（尊重段落/换行/句号等自然边界）
 * 4. 元数据增强
 * 5. 向量化入库
 */
@Slf4j
@Configuration
public class RageConfig implements CommandLineRunner {

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Resource
    private RagTools ragTools;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Value("${files.docs.path:src/main/resources/docs/}")
    private String docsPath;

    @Value("${rag.global-knowledge.enabled:true}")
    private boolean globalKnowledgeEnabled;

    @Value("${rag.global-knowledge.namespace:global-knowledge}")
    private String globalKnowledgeNamespace;

    @Value("${rag.splitter.chunk-size:500}")
    private int chunkSize;

    @Value("${rag.splitter.overlap:100}")
    private int overlap;

    /**
     * 应用启动后自动执行全局知识库初始化
     */
    @Override
    public void run(String... args) {
        if (globalKnowledgeEnabled) {
            log.info("=== 开始初始化全局知识库 === namespace={}", globalKnowledgeNamespace);
            try {
                ingestDocumentsForNamespace(globalKnowledgeNamespace);
                log.info("=== 全局知识库初始化成功 ===");
            } catch (Exception e) {
                log.error("=== 全局知识库初始化失败 ===", e);
                // 不阻止应用启动，仅记录错误
            }
        } else {
            log.info("全局知识库初始化已禁用（配置：rag.global-knowledge.enabled=false）");
        }
    }


    /**
     * 默认的全局内容检索器（用于全局知识库）
     * 注：个人知识库使用动态 namespace，不依赖此 Bean
     */
    @Bean
    public ContentRetriever contentRetriever() {
        log.info("=== 创建全局内容检索器 === namespace={}", globalKnowledgeNamespace);
        try {
            return createContentRetrieverForNamespace(globalKnowledgeNamespace, 5, 0.75);
        } catch (Exception e) {
            log.warn("=== 全局内容检索器创建失败（Pinecone 连接问题），返回空检索器 ===", e);
            // 返回一个安全的空检索器：设置 maxResults=0 确保不返回结果，同时提供必要的依赖避免NPE
            return EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(ragTools.getEmbeddingStore(globalKnowledgeNamespace))
                    .embeddingModel(qwenEmbeddingModel)
                    .maxResults(0)
                    .minScore(1.0)
                    .build();
        }
    }

    /**
     * 为指定 namespace 创建内容检索器
     *
     * @param namespace Pinecone namespace
     * @param maxResults 最大返回结果数
     * @param minScore 最小相似度分数
     * @return ContentRetriever
     */
    public ContentRetriever createContentRetrieverForNamespace(String namespace, int maxResults, double minScore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(ragTools.getEmbeddingStore(namespace))
                .embeddingModel(qwenEmbeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    /**
     * 加载并处理文档（用于初始化全局知识库）
     * 个人知识库的文档处理在 KnowledgeDocumentServiceImpl 中异步处理
     * <p>
     * 增强版流程：文档加载 → 文档清洗 → 智能分片 → 元数据增强 → 向量化入库
     */
    public void ingestDocumentsForNamespace(String namespace) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== 开始向量化处理 === namespace={}", namespace);

            // 1. 加载文档
            log.info("步骤1: 开始加载文档，路径={}", docsPath);
            Path docsDir = Paths.get(docsPath);
            List<Document> documents = FileSystemDocumentLoader.loadDocuments(docsDir);
            log.info("文档加载完成！共加载{}个文档", documents.size());

            if (documents.isEmpty()) {
                log.warn("未找到文档，跳过向量化处理");
                return;
            }

            // 2. 文档清洗（递归分片友好的清洗方式）
            log.info("步骤2: 开始文档清洗");
            documents = DocumentCleanHandler.clearDocumentsForRecursiveSplit(documents);
            log.info("文档清洗完成！");

            // 3. 初始化 EmbeddingStore
            log.info("步骤3: 初始化向量存储，namespace={}", namespace);
            var embeddingStore = ragTools.getEmbeddingStore(namespace);
            log.info("向量存储初始化成功");

            // 4. 构建 Ingestor（使用智能分片器）
            log.info("步骤4: 构建向量化处理器（智能分片: chunkSize={}, overlap={})", chunkSize, overlap);
            SmartDocumentSplitter splitter = new SmartDocumentSplitter(chunkSize, overlap);

            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(splitter)
                    .textSegmentTransformer(textSegment -> {
                        Metadata metadata = textSegment.metadata();
                        String fileName = (metadata != null) ? metadata.getString("file_name") : null;
                        String prefix = (fileName != null) ? "[文件: " + fileName + "]\n" : "";
                        // 添加文件名到文本开头，帮助模型识别来源
                        return TextSegment.from(prefix + textSegment.text(), metadata);
                    })
                    .embeddingModel(qwenEmbeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();
            log.info("向量化处理器构建完成");

            // 5. 执行向量化（最耗时步骤）
            log.info("步骤5: 开始执行向量化，预计耗时较长...");
            ingestor.ingest(documents);

            long duration = System.currentTimeMillis() - startTime;
            log.info("文档向量化完成！namespace={}, 文档数={}, 耗时={}ms",
                    namespace, documents.size(), duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("文档处理失败: namespace={}, 耗时={}ms", namespace, duration, e);
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        }
    }
}
