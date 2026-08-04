package com.project.platform.controller.file;

import com.project.platform.service.FileService;
import com.project.platform.vo.FileInfoVO;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/file")
@Slf4j
public class FilesController {

    @Resource
    private FileService fileService;


    @PostMapping("/upload")
    public ResponseVO<FileInfoVO> upload(@RequestParam MultipartFile file) throws IOException, NoSuchAlgorithmException {
        return ResponseVO.ok(fileService.upload(file));
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<org.springframework.core.io.Resource> get(@PathVariable("fileName") String filename) throws IOException {
        // 支持路径中包含子目录，例如 multimedia/20260420_xxx.jpg
        File file = fileService.getFile(filename);
        if (file.exists() && !file.isDirectory()) {
            org.springframework.core.io.Resource resource = new FileSystemResource(file);

            // 根据文件扩展名设置正确的 ContentType
            String contentType = getContentType(filename);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(file.length())
                    .body(resource);
        } else {
            log.warn("=== 文件不存在：{}", filename);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * 根据文件名获取 ContentType
     */
    private String getContentType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerName.endsWith(".md")) {
            return "text/markdown";
        }
        return "application/octet-stream";
    }
}
