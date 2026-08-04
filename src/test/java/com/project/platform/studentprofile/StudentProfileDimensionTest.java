package com.project.platform.studentprofile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 学生画像维度更新测试
 * 验证新的11个维度字段能正确解析和处理
 */
@SpringBootTest
public class StudentProfileDimensionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testNewDimensionJsonParsing() throws Exception {
        // 模拟AI返回的新格式JSON
        String jsonStr = "{\n" +
                "  \"knowledgeDepth\": 4,\n" +
                "  \"conceptClarity\": 3,\n" +
                "  \"memoryStability\": 4,\n" +
                "  \"transferAbility\": 3,\n" +
                "  \"structuralIntegrity\": 4,\n" +
                "  \"informationProcessing\": 3,\n" +
                "  \"knowledgeRepresentation\": 5,\n" +
                "  \"learningInitiative\": 4,\n" +
                "  \"cognitiveControl\": 3,\n" +
                "  \"practiceOrientation\": 4,\n" +
                "  \"taskManagement\": 3,\n" +
                "  \"learningStyle\": \"visual,kinesthetic\",\n" +
                "  \"weaknessPoints\": [\"知识点1\", \"知识点2\"],\n" +
                "  \"preferredStyle\": \"project-based\",\n" +
                "  \"summary\": \"测试总结\"\n" +
                "}";

        JsonNode jsonNode = objectMapper.readTree(jsonStr);

        // 验证知识基础维度
        assertEquals(4, jsonNode.path("knowledgeDepth").asInt());
        assertEquals(3, jsonNode.path("conceptClarity").asInt());
        assertEquals(4, jsonNode.path("memoryStability").asInt());
        assertEquals(3, jsonNode.path("transferAbility").asInt());
        assertEquals(4, jsonNode.path("structuralIntegrity").asInt());

        // 验证认知风格维度
        assertEquals(3, jsonNode.path("informationProcessing").asInt());
        assertEquals(5, jsonNode.path("knowledgeRepresentation").asInt());
        assertEquals(4, jsonNode.path("learningInitiative").asInt());
        assertEquals(3, jsonNode.path("cognitiveControl").asInt());
        assertEquals(4, jsonNode.path("practiceOrientation").asInt());
        assertEquals(3, jsonNode.path("taskManagement").asInt());

        // 验证其他字段
        assertEquals("visual,kinesthetic", jsonNode.path("learningStyle").asText());
        assertEquals("project-based", jsonNode.path("preferredStyle").asText());
        assertEquals("测试总结", jsonNode.path("summary").asText());
        
        // 验证薄弱知识点
        assertTrue(jsonNode.has("weaknessPoints"));
        assertTrue(jsonNode.get("weaknessPoints").isArray());
        assertEquals(2, jsonNode.get("weaknessPoints").size());
    }

    @Test
    public void testScoreConversion() {
        // 测试1-5分转换为0-100分的逻辑
        int[] scores = {1, 2, 3, 4, 5};
        int[] expectedPercentages = {20, 40, 60, 80, 100};

        for (int i = 0; i < scores.length; i++) {
            int percentage = convertScoreToPercentage(scores[i]);
            assertEquals(expectedPercentages[i], percentage, 
                "分数 " + scores[i] + " 应该转换为 " + expectedPercentages[i] + " 分");
        }
    }

    /**
     * 将1-5分转换为0-100分
     */
    private int convertScoreToPercentage(int score) {
        if (score < 1) score = 1;
        if (score > 5) score = 5;
        return (score - 1) * 25; // 1->0, 2->25, 3->50, 4->75, 5->100
    }

    @Test
    public void testDefaultValues() throws Exception {
        // 测试缺少某些字段时的默认值处理
        String jsonStr = "{\n" +
                "  \"knowledgeDepth\": 3,\n" +
                "  \"learningStyle\": \"visual\"\n" +
                "}";

        JsonNode jsonNode = objectMapper.readTree(jsonStr);

        // 验证存在的字段
        assertEquals(3, jsonNode.path("knowledgeDepth").asInt());
        assertEquals("visual", jsonNode.path("learningStyle").asText());

        // 验证缺失字段的默认值（应该返回0或空）
        assertEquals(0, jsonNode.path("conceptClarity").asInt());
        assertEquals("", jsonNode.path("memoryStability").asText());
    }
}
