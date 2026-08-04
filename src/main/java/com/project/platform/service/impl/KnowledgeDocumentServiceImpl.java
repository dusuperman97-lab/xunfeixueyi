package com.project.platform.service.impl;

import com.project.platform.agents.config.utils.RagTools;
import com.project.platform.agents.config.utils.Tools;
import com.project.platform.entity.KnowledgeBase;
import com.project.platform.entity.KnowledgeDocument;
import com.project.platform.mapper.KnowledgeBaseMapper;
import com.project.platform.mapper.KnowledgeDocumentMapper;
import com.project.platform.rag.ingest.DocumentCleanHandler;
import com.project.platform.rag.ingest.SmartDocumentSplitter;
import com.project.platform.rag.store.VectorCleanupService;
import com.project.platform.service.KnowledgeDocumentService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private RagTools ragTools;

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Resource
    private VectorCleanupService vectorCleanupService;

    @Value("${files.uploads.path:uploads}")
    private String uploadBasePath;
    @Autowired
    private Tools tools;

    @Override
    public KnowledgeDocument uploadDocument(Long kbId, Long studentId, MultipartFile file) {
        // 1. 验证知识库是否存在
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || !kb.getStudentId().equals(studentId)) {
            throw new RuntimeException("知识库不存在或无权限");
        }

        // 2. 验证文件类型
        String originalFilename = file.getOriginalFilename();
        String fileType = getFileType(originalFilename);
        if (!isValidFileType(fileType)) {
            throw new RuntimeException("不支持的文件类型，仅支持: PDF, DOCX, DOC, TXT, MD");
        }

        // 3. 保存文件到本地
        String filePath = saveFile(kbId, file, fileType);

        // 4. 创建文档记录
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setKbId(kbId);
        doc.setStudentId(studentId);
        doc.setFileName(originalFilename);
        doc.setFilePath(filePath);
        doc.setFileSize(file.getSize());
        doc.setFileType(fileType);
        doc.setEmbeddingStatus(0); // 未处理
        doc.setStatus(1);
        
        knowledgeDocumentMapper.insert(doc);
        log.info("文档上传成功: id={}, fileName={}, kbId={}", doc.getId(), originalFilename, kbId);

        // 5. 异步处理文档解析和向量化
        processDocumentAsync(doc, kb.getPineconeNamespace());

        return doc;
    }

    /**
     * 异步处理文档：解析 -> 分片 -> 向量化
     */
    private void processDocumentAsync(KnowledgeDocument doc, String namespace) {
        new Thread(() -> {
            try {
                // 更新状态为处理中
                doc.setEmbeddingStatus(1);
                knowledgeDocumentMapper.update(doc);

                // 查询知识库名称（用于添加到 metadata）
                KnowledgeBase kb = knowledgeBaseMapper.selectById(doc.getKbId());
                String kbName = kb != null ? kb.getName() : "未知知识库";

                // 1. 解析文档
                Document document = parseDocument(doc.getFilePath(), doc.getFileType());
                log.info("文档解析成功: {}, 内容长度: {}", doc.getFileName(), document.text().length());

                // 设置内容摘要（取前200字符）
                String summary = document.text().length() > 200 
                    ? document.text().substring(0, 200) + "..." 
                    : document.text();
                doc.setContentSummary(summary);

                // 2. 文档清洗（去除冗余空白、重复段落）
                List<Document> docs = List.of(document);
                docs = DocumentCleanHandler.clearDocumentsForRecursiveSplit(docs);
                document = docs.get(0);
                log.info("文档清洗完成: {}", doc.getFileName());

                // 3. 文档分片和向量化（使用智能分片器，尊重自然边界）
                SmartDocumentSplitter splitter = new SmartDocumentSplitter(500, 100);

                // 先分片，获取分片数量
                List<TextSegment> segments = splitter.split(document);
                int segmentCount = segments.size();
                log.info("文档分片完成: {}, 分片数: {}", doc.getFileName(), segmentCount);

                // 使用用户知识库的 namespace
                EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                        .documentSplitter(splitter)
                        .textSegmentTransformer(textSegment -> {
                            // 添加元数据：知识库ID、知识库名称、文档ID、文件名
                            textSegment.metadata().put("kb_id", doc.getKbId().toString());
                            textSegment.metadata().put("kb_name", kbName);
                            textSegment.metadata().put("doc_id", doc.getId().toString());
                            textSegment.metadata().put("file_name", doc.getFileName());
                            textSegment.metadata().put("student_id", doc.getStudentId().toString());
                            return textSegment;
                        })
                        .embeddingModel(qwenEmbeddingModel)
                        .embeddingStore(ragTools.getEmbeddingStore(namespace))
                        .build();

                // 注意：这里需要使用带 namespace 的 EmbeddingStore
                // LangChain4j 的 Pinecone 支持通过 metadata 过滤实现多租户
                //加载文档
                ingestor.ingest(document);
                
                // 计算分片数量（这里简化处理，实际应该从 ingestor 获取）
                doc.setSegmentCount(segmentCount);
                doc.setEmbeddingStatus(2); // 已完成
                knowledgeDocumentMapper.update(doc);

                log.info("文档向量化完成: {}, 分片数: {}", doc.getFileName(), doc.getSegmentCount());

            } catch (Exception e) {
                log.error("文档处理失败: {}", doc.getFileName(), e);
                doc.setEmbeddingStatus(3); // 失败
                knowledgeDocumentMapper.update(doc);
            }
        }).start();
    }

    /**
     * 解析文档
     */
    private Document parseDocument(String filePath, String fileType) {
        try {
            // 将相对路径转换为绝对路径
            Path basePath = Paths.get(uploadBasePath).toAbsolutePath().normalize();
            Path absolutePath = basePath.resolve(filePath).normalize();
            
            // 防止路径遍历攻击
            if (!absolutePath.startsWith(basePath)) {
                throw new RuntimeException("非法的文件路径");
            }
            
            File file = absolutePath.toFile();
            if (!file.exists()) {
                throw new RuntimeException("文件不存在: " + absolutePath);
            }
            
            return switch (fileType.toLowerCase()) {
                case "pdf" -> {
                    ApachePdfBoxDocumentParser parser = new ApachePdfBoxDocumentParser();
                    yield FileSystemDocumentLoader.loadDocument(file.toPath(), parser);
                }
                case "docx", "doc" -> {
                    ApachePoiDocumentParser parser = new ApachePoiDocumentParser();
                    yield FileSystemDocumentLoader.loadDocument(file.toPath(), parser);
                }
                case "txt", "md" -> FileSystemDocumentLoader.loadDocument(file.toPath());
                default -> throw new RuntimeException("不支持的文件类型: " + fileType);
            };
        } catch (Exception e) {
            log.error("文档解析失败: {}", filePath, e);
            throw new RuntimeException("文档解析失败: " + e.getMessage());
        }
    }

    /**
     * 保存文件
     */
    private String saveFile(Long kbId, MultipartFile file, String fileType) {
        try {
            // 转换为绝对路径
            Path basePath = Paths.get(uploadBasePath).toAbsolutePath().normalize();

            // 创建目录结构: uploads/knowledge/{kbId}/{date}/
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Path kbDir = basePath.resolve("knowledge")
                    .resolve(kbId.toString())
                    .resolve(dateStr)
                    .normalize();

            // 防止路径遍历攻击
            if (!kbDir.startsWith(basePath)) {
                throw new RuntimeException("非法的文件路径");
            }

            // 强制创建所有父目录
            if (!Files.exists(kbDir)) {
                Files.createDirectories(kbDir);
                log.info("创建目录: {}", kbDir);
            }

            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + "." + fileType;
            Path filePath = kbDir.resolve(fileName);

            file.transferTo(filePath.toFile());
            
            // 返回相对路径（相对于项目根目录）
            Path relativePath = basePath.relativize(filePath);
            String relativePathStr = relativePath.toString().replace("\\", "/");
            
            log.info("文件保存成功，相对路径: {}", relativePathStr);
            return relativePathStr;
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件类型
     */
    private String getFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new RuntimeException("文件名无效");
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 验证文件类型
     */
    private boolean isValidFileType(String fileType) {
        return List.of("pdf", "docx", "doc", "txt", "md").contains(fileType.toLowerCase());
    }

    @Override
    public List<KnowledgeDocument> getDocumentsByKbId(Long kbId, Long studentId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || !kb.getStudentId().equals(studentId)) {
            throw new RuntimeException("知识库不存在或无权限");
        }
        return knowledgeDocumentMapper.selectByKbId(kbId);
    }

    @Override
    public void deleteDocument(Long documentId, Long studentId) {
        KnowledgeDocument doc = knowledgeDocumentMapper.selectById(documentId);
        if (doc == null || !doc.getStudentId().equals(studentId)) {
            throw new RuntimeException("文档不存在或无权限");
        }

        // 1. 获取对应知识库的 namespace
        KnowledgeBase kb = knowledgeBaseMapper.selectById(doc.getKbId());
        String namespace = kb != null ? kb.getPineconeNamespace() : null;

        // 2. 清理 Pinecone 中的向量数据 — 按 doc_id 精确删除
        if (namespace != null) {
            try {
                vectorCleanupService.deleteVectorsByDocId(documentId, namespace);
                log.info("文档向量数据清理完成: docId={}, namespace={}", documentId, namespace);
            } catch (Exception e) {
                log.error("清理文档向量数据失败: docId={}", documentId, e);
                // 清理失败不阻塞主流程，记录日志供人工处理
            }
        } else {
            log.warn("文档关联的知识库无 namespace，跳过向量清理: docId={}, kbId={}",
                    documentId, doc.getKbId());
        }

        // 3. 逻辑删除文档记录
        knowledgeDocumentMapper.deleteById(documentId);

        // 4. 删除物理文件
        try {
            Path basePath = Paths.get(uploadBasePath).toAbsolutePath().normalize();
            Path absolutePath = basePath.resolve(doc.getFilePath()).normalize();

            if (!absolutePath.startsWith(basePath)) {
                log.warn("文件路径越界，跳过删除: {}", doc.getFilePath());
                return;
            }

            File file = absolutePath.toFile();
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    log.info("物理文件删除成功: {}", doc.getFilePath());
                } else {
                    log.warn("物理文件删除失败: {}", doc.getFilePath());
                }
            }
        } catch (Exception e) {
            log.error("删除物理文件异常: {}", doc.getFilePath(), e);
        }

        log.info("删除文档成功: id={}", documentId);
    }

    @Override
    public List<KnowledgeDocument> getDocumentsByStudentId(Long studentId) {
        return knowledgeDocumentMapper.selectByStudentId(studentId);
    }

    @Override
    public List<TextSegment> getDocumentSegments(Long documentId, Long studentId) {
        KnowledgeDocument doc = knowledgeDocumentMapper.selectById(documentId);
        if (doc == null || !doc.getStudentId().equals(studentId)) {
            throw new RuntimeException("文档不存在或无权限");
        }
        if (doc.getEmbeddingStatus() != 2) {
            throw new RuntimeException("文档尚未完成向量化，无法查看分片");
        }

        // 从 Pinecone 中查询该文档的所有分片
        KnowledgeBase kb = knowledgeBaseMapper.selectById(doc.getKbId());
        if (kb == null) {
            throw new RuntimeException("关联的知识库不存在");
        }

        try {
            // 解析文档获取分片列表（从已存储的向量库中按 doc_id 过滤查询）
            Document document = parseDocument(doc.getFilePath(), doc.getFileType());
            List<Document> docs = List.of(document);
            docs = DocumentCleanHandler.clearDocumentsForRecursiveSplit(docs);
            document = docs.get(0);

            SmartDocumentSplitter splitter = new SmartDocumentSplitter(500, 100);
            return splitter.split(document);
        } catch (Exception e) {
            log.error("获取文档分片失败: docId={}", documentId, e);
            throw new RuntimeException("获取文档分片失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteDocumentSegment(Long documentId, Long studentId, int chunkIndex) {
        KnowledgeDocument doc = knowledgeDocumentMapper.selectById(documentId);
        if (doc == null || !doc.getStudentId().equals(studentId)) {
            throw new RuntimeException("文档不存在或无权限");
        }

        KnowledgeBase kb = knowledgeBaseMapper.selectById(doc.getKbId());
        if (kb == null) {
            throw new RuntimeException("关联的知识库不存在");
        }

        // 从 Pinecone 中删除指定 chunk_index 的分片向量
        try {
            vectorCleanupService.deleteByMetadataAndChunkIndex(
                    documentId, kb.getPineconeNamespace(), chunkIndex);
            log.info("文档分片删除成功: docId={}, chunkIndex={}", documentId, chunkIndex);
        } catch (Exception e) {
            log.error("删除文档分片失败: docId={}, chunkIndex={}", documentId, chunkIndex, e);
            throw new RuntimeException("删除文档分片失败: " + e.getMessage());
        }
    }

    @Override
    public KnowledgeDocument reembedDocument(Long documentId, Long studentId) {
        KnowledgeDocument doc = knowledgeDocumentMapper.selectById(documentId);
        if (doc == null || !doc.getStudentId().equals(studentId)) {
            throw new RuntimeException("文档不存在或无权限");
        }

        KnowledgeBase kb = knowledgeBaseMapper.selectById(doc.getKbId());
        if (kb == null) {
            throw new RuntimeException("关联的知识库不存在");
        }

        // 1. 先清理旧的向量数据
        try {
            vectorCleanupService.deleteVectorsByDocId(documentId, kb.getPineconeNamespace());
            log.info("旧向量数据清理完成: docId={}, namespace={}", documentId, kb.getPineconeNamespace());
        } catch (Exception e) {
            log.warn("旧向量数据清理失败（可能不存在），继续执行: docId={}", documentId, e);
        }

        // 2. 重新执行向量化
        doc.setEmbeddingStatus(1); // 处理中
        knowledgeDocumentMapper.update(doc);

        try {
            processDocumentSync(doc, kb);
            doc.setEmbeddingStatus(2); // 已完成
            knowledgeDocumentMapper.update(doc);
            log.info("文档重新向量化完成: id={}, fileName={}", doc.getId(), doc.getFileName());
        } catch (Exception e) {
            log.error("文档重新向量化失败: id={}", doc.getId(), e);
            doc.setEmbeddingStatus(3); // 失败
            doc.setErrorMessage(e.getMessage());
            knowledgeDocumentMapper.update(doc);
            throw new RuntimeException("文档重新向量化失败: " + e.getMessage());
        }

        return doc;
    }

    /**
     * 同步处理文档向量化（用于重新向量化场景）
     */
    private void processDocumentSync(KnowledgeDocument doc, KnowledgeBase kb) {
        String kbName = kb.getName() != null ? kb.getName() : "未知知识库";

        // 1. 解析文档
        Document document = parseDocument(doc.getFilePath(), doc.getFileType());
        log.info("文档解析成功: {}, 内容长度: {}", doc.getFileName(), document.text().length());

        String summary = document.text().length() > 200
                ? document.text().substring(0, 200) + "..."
                : document.text();
        doc.setContentSummary(summary);

        // 2. 文档清洗
        List<Document> docs = List.of(document);
        docs = DocumentCleanHandler.clearDocumentsForRecursiveSplit(docs);
        document = docs.get(0);

        // 3. 分片 + 向量化
        SmartDocumentSplitter splitter = new SmartDocumentSplitter(500, 100);
        List<TextSegment> segments = splitter.split(document);
        int segmentCount = segments.size();
        log.info("文档分片完成: {}, 分片数: {}", doc.getFileName(), segmentCount);

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .textSegmentTransformer(textSegment -> {
                    textSegment.metadata().put("kb_id", doc.getKbId().toString());
                    textSegment.metadata().put("kb_name", kbName);
                    textSegment.metadata().put("doc_id", doc.getId().toString());
                    textSegment.metadata().put("file_name", doc.getFileName());
                    textSegment.metadata().put("student_id", doc.getStudentId().toString());
                    return textSegment;
                })
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(ragTools.getEmbeddingStore(kb.getPineconeNamespace()))
                .build();

        ingestor.ingest(document);
        doc.setSegmentCount(segmentCount);
    }
}
