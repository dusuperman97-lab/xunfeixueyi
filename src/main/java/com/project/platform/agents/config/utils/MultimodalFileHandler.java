package com.project.platform.agents.config.utils;

import com.project.platform.utils.AliOssUtil;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 多模态文件处理工具类
 * 支持图片Base64转换、文档文本提取(txt, doc, docx, pdf)
 */
@Component
@Slf4j
public class MultimodalFileHandler {

    @Value("${files.uploads.path:uploads/}")
    private String uploadPath;

    @Value("${files.imageUrl}")
    private String imageUrlPrefix;

    @Autowired(required = false)
    private AliOssUtil aliOssUtil;

    private static final String MULTIMEDIA_DIR = "multimedia";

    /**
     * 保存图片文件并返回公网可访问的 URL
     * 【优先使用阿里云OSS，如果未配置则使用本地存储】
     */
    public String saveImage(MultipartFile file) {
        try {
            String fileName = generateFileName(file.getOriginalFilename());

            // 如果配置了阿里云OSS，优先上传到OSS
            if (aliOssUtil != null) {
                log.info("=== 使用阿里云OSS上传图片: {}", fileName);
                byte[] bytes = file.getBytes();
                String ossObjectKey = MULTIMEDIA_DIR + "/" + fileName;
                String ossUrl = aliOssUtil.upload(bytes, ossObjectKey);
                log.info("=== 图片上传OSS成功，URL: {}", ossUrl);
                return ossUrl;
            } else {
                // 降级方案：保存到本地
                log.warn("=== 未配置阿里云OSS，使用本地存储保存图片: {}", fileName);
                Path basePath = Paths.get(uploadPath).toAbsolutePath();
                Path targetDir = basePath.resolve(MULTIMEDIA_DIR);
                Files.createDirectories(targetDir);

                Path filePath = targetDir.resolve(fileName);
                file.transferTo(filePath.toFile());

                // 返回完整的公网 URL，确保大模型可以访问
                String publicUrl = imageUrlPrefix + MULTIMEDIA_DIR + "/" + fileName;
                log.info("=== 图片保存成功（本地），公网 URL: {}", publicUrl);
                return publicUrl;
            }
        } catch (Exception e) {
            log.error("保存图片失败", e);
            throw new RuntimeException("保存图片失败: " + e.getMessage());
        }
    }


    /**
     * 保存文档文件并返回URL
     */
    public String saveDocument(MultipartFile file) {
        try {
            String fileName = generateFileName(file.getOriginalFilename());

            Path basePath = Paths.get(uploadPath).toAbsolutePath();
            Path targetDir = basePath.resolve(MULTIMEDIA_DIR);
            Files.createDirectories(targetDir);

            Path filePath = targetDir.resolve(fileName);
            file.transferTo(filePath.toFile());

            String fileUrl = "/uploads/" + MULTIMEDIA_DIR + "/" + fileName;
            log.info("=== 文档保存成功: {}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("保存文档失败", e);
            throw new RuntimeException("保存文档失败: " + e.getMessage());
        }
    }

    /**
     * 从文档中提取文本内容
     * 支持: txt, doc, docx, pdf
     */
    public String extractTextFromDocument(String fileUrl) {
        try {
            String fullPath = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            Path path = Paths.get(fullPath);

            if (!Files.exists(path)) {
                log.warn("=== 文档文件不存在: {}", fullPath);
                return null;
            }

            String fileName = path.getFileName().toString().toLowerCase();

            if (fileName.endsWith(".txt")) {
                return extractFromTxt(path);
            } else if (fileName.endsWith(".doc")) {
                return extractFromDoc(path);
            } else if (fileName.endsWith(".docx")) {
                return extractFromDocx(path);
            } else if (fileName.endsWith(".pdf")) {
                return extractFromPdf(path);
            } else {
                log.warn("=== 不支持的文件格式: {}", fileName);
                return null;
            }
        } catch (Exception e) {
            log.error("文档文本提取失败", e);
            return null;
        }
    }

    /**
     * 提取TXT文件内容
     */
    private String extractFromTxt(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /**
     * 提取DOC文件内容（Word 2003）
     */
    private String extractFromDoc(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path);
             HWPFDocument doc = new HWPFDocument(is);
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        }
    }

    /**
     * 提取DOCX文件内容（Word 2007+）
     */
    private String extractFromDocx(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path);
             XWPFDocument docx = new XWPFDocument(is)) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph para : docx.getParagraphs()) {
                text.append(para.getText()).append("\n");
            }
            return text.toString();
        }
    }

    /**
     * 提取PDF文件内容
     */
    private String extractFromPdf(Path path) throws IOException {
        try (PDDocument document = PDDocument.load(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 根据文件扩展名获取MIME类型
     */
    private String getMimeType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerName.endsWith(".bmp")) {
            return "image/bmp";
        }
        return "image/jpeg"; // 默认
    }

    /**
     * 生成唯一文件名
     */
    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
    }

    /**
     * 检查文件类型是否为支持的图片
     */
    public static boolean isSupportedImage(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".gif") ||
                lower.endsWith(".webp") || lower.endsWith(".bmp");
    }

    /**
     * 检查文件类型是否为支持的文档
     */
    public static boolean isSupportedDocument(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".doc") ||
                lower.endsWith(".docx") || lower.endsWith(".pdf");
    }

    /**
     * 构建包含多模态内容的LangChain4j UserMessage对象
     * 【通用模式】：使用图片URL方式（适用于支持URL的大模型）
     * - 直接使用图片URL，节省Token消耗
     * - 文档内容会提取为文本并添加到消息中
     *
     * @param textMessage 用户输入的文本消息
     * @param imageUrls 图片公网 URL 列表（直接使用）
     * @param docUrls 文档 URL 列表
     * @return LangChain4j的UserMessage对象,可直接传递给AI模型
     */
    public UserMessage buildMultimodalUserMessage(String textMessage,
                                                  List<String> imageUrls,
                                                  List<String> docUrls) {
        List<Content> contents = new ArrayList<>();

        // 1. 添加文本文档内容(提取为文本)
        if (docUrls != null && !docUrls.isEmpty()) {
            StringBuilder docText = new StringBuilder();
            docText.append("【用户上传的文档内容】\n");

            for (String docUrl : docUrls) {
                String extractedText = extractTextFromDocument(docUrl);
                if (extractedText != null && !extractedText.isEmpty()) {
                    String fileName = docUrl.substring(docUrl.lastIndexOf("/") + 1);
                    docText.append("\n--- 文件: ").append(fileName).append(" ---\n");
                    // 限制文档文本长度
                    int maxDocLength = 10000;
                    if (extractedText.length() > maxDocLength) {
                        docText.append(extractedText.substring(0, maxDocLength))
                                .append("...（文档过长，已截取前" + maxDocLength + "字）\n");
                    } else {
                        docText.append(extractedText).append("\n");
                    }
                }
            }
            docText.append("\n");
            contents.add(TextContent.from(docText.toString()));
        }

        // 2. 添加图片内容 - 【使用URL方式，节省Token】
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String imageUrl : imageUrls) {
                try {
                    // 直接使用图片URL
                    contents.add(ImageContent.from(imageUrl));
                    log.info("=== 成功添加图片URL到消息: {}", imageUrl);
                } catch (Exception e) {
                    log.error("=== 处理图片失败: {}", imageUrl, e);
                }
            }
        }

        // 3. 添加用户文本消息
        if (textMessage != null && !textMessage.isEmpty()) {
            contents.add(TextContent.from(textMessage));
        }

        // 如果没有任何内容,至少返回一个空文本
        if (contents.isEmpty()) {
            contents.add(TextContent.from(""));
        }

        return UserMessage.from(contents);
    }

}
