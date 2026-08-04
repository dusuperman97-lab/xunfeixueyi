package com.project.platform.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {
    /**
     * 阿里云文件上传工具类
     *
     * @param endpoint          访问点
     * @param accessKeyId       访问id
     * @param accessKeySecret   访问密钥
     * @param bucketName        存储空间名称
     */
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes
     * @param objectName
     * @return OSS访问URL
     */
    public String upload(byte[] bytes, String objectName) {
        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 创建PutObject请求。将文件上传到OSS中
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
            log.info("文件上传成功: bucket={}, object={}", bucketName, objectName);
        } catch (OSSException oe) {
            log.error("OSS服务异常 - Error Message: {}, Error Code: {}, Request ID: {}", 
                    oe.getErrorMessage(), oe.getErrorCode(), oe.getRequestId());
            throw new RuntimeException("文件上传到OSS失败: " + oe.getErrorMessage());
        } catch (ClientException ce) {
            log.error("客户端异常 - Error Message: {}", ce.getMessage());
            throw new RuntimeException("文件上传到OSS失败: " + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        // 阿里云OSS文件访问路径规则 https://BucketName.Endpoint/ObjectName
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".")
                .append(endpoint)
                .append("/")
                .append(objectName);

        String fileUrl = stringBuilder.toString();
        log.info("文件上传到OSS成功，访问URL: {}", fileUrl);

        return fileUrl;
    }
}
