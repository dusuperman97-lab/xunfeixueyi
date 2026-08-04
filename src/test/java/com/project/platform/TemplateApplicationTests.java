package com.project.platform;

import com.project.platform.agents.ai.AiCodeHelperService;
import com.project.platform.agents.config.utils.Tools;
import com.project.platform.agents.tools.*;
import com.project.platform.entity.Question;
import com.project.platform.service.QuestionService;
import com.project.platform.service.impl.CourseGenerationServiceImpl;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@SpringBootTest
class TemplateApplicationTests {

        @Autowired
        private AiCodeHelperService aiCodeHelperService;

        @Value("${files.uploads.path}")
        private String uploadsPath;

        @Value("${files.uploads.baseUrl}")
        private String serverDomain;

        @Resource
        private BilibiliVideoTool bilibiliSearchTool;

        @Resource
        private Tools tools;

        @Resource
        private QuestionService questionService;





        @Test
        public void test(){
                BilibiliVideoTool bilibiliSearchTool = new BilibiliVideoTool();
                String result = bilibiliSearchTool.searchVideos("ragGraph");
                System.out.println(result);

        }

        @Test
        public void test2(){
                ImageSearchTool imageSearchTool = new ImageSearchTool();
                List<String> result = imageSearchTool.searchImage("DNA结构图");
//                String result = imageSearchTool.searchTechnicalDiagram("python");
                System.out.println(result);

        }
        @Test
        public void test3(){
                ImageSearchTool3 imageSearchTool = new ImageSearchTool3();
                String result = imageSearchTool.searchImage("机器学习算法原理");
                System.out.println(result);

        }

        @Test
        public void test5(){
                ImageSearchTool2 imageSearchTool = new ImageSearchTool2();
                List<String> result = imageSearchTool.searchImage("安全考虑 示意图");
                System.out.println(result);

        }
        @Test
        public void test4(){
                ImageSearchTool3 bilibiliSearchTool = new ImageSearchTool3();
                String result = bilibiliSearchTool.searchImage("Java继承关系图");
                System.out.println(result);

        }

        @Test
        public void test6(){
                KnowledgeSearchTool3 knowledgeSearchTool = new KnowledgeSearchTool3();
                String result = knowledgeSearchTool.searchKnowledge("人工智能");
                System.out.println(result);
        }

        @Test
        public void test7(){
                KnowledgeSearchTool knowledgeSearchTool = new KnowledgeSearchTool();
                String result = knowledgeSearchTool.searchKnowledge("机器学习算法原理相关文献");
                System.out.println(result);
        }

        @Test
        void chatWithMessage(){
                UserMessage userMessage = UserMessage.from(
                        TextContent.from("直接描述图片，是哪吒电影的海报吗"),
                        ImageContent.from("https://aliyun-database.oss-cn-shenzhen.aliyuncs.com/multimedia/20260420_164925_06a71ce7.jpg")
                );
                System.out.println(aiCodeHelperService.chatWithImage(userMessage));
        }

        @Test
        void test8(){
                System.out.println(bilibiliSearchTool.searchVideos("人工智能"));
        }

        @Test
        void test9() throws IOException {
                String imageUrl = "https://img1.baidu.com/it/u=2194317428,1217511125&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=889";
                String localPath = tools.saveImageUrl(imageUrl);
                System.out.println("图片下载成功，本地路径为：");
                System.out.println(localPath);

        }
        @Test
        void test10() throws IOException {
                // 1. 创建 imag 文件夹
                String projectPath = System.getProperty("user.dir");

                // 保存目录：项目根目录/uploads/imag
                Path dirPath = Paths.get(projectPath, "uploads", "imag");
                System.out.println(dirPath);

        }


}
