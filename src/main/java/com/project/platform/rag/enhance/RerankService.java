package com.project.platform.rag.enhance;

import dev.langchain4j.rag.content.Content;

import java.util.List;

/**
 * 重排序服务接口
 * <p>
 * 为什么要重排序？
 * 向量检索用的是双塔模型（Bi-Encoder），速度快但精度有限。
 * Rerank用的是交叉编码器（Cross-Encoder），同时考虑query和document的交互，
 * 精度远高于向量相似度，但速度慢，所以只在候选集上做二次排序。
 */
public interface RerankService {

    /**
     * 对候选内容进行重排序
     *
     * @param query     用户查询
     * @param contents  候选内容列表
     * @param topN      返回前N个结果
     * @return 重排序后的内容列表
     */
    List<Content> rerank(String query, List<Content> contents, int topN);
}
