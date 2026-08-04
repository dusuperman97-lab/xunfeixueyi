package com.project.platform.rag.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RAG Pipeline 监控埋点采集器
 * <p>
 * 对 Pipeline 各阶段耗时、调用次数、降级次数进行埋点上报，
 * 对接 Prometheus + Grafana 监控大盘。
 */
@Component
public class RagMetricsCollector {

    private final MeterRegistry registry;

    // === Timers - 各阶段耗时 ===
    private final Timer rewriteTimer;
    private final Timer hydeTimer;
    private final Timer retrievalTimer;
    private final Timer filterTimer;
    private final Timer rerankTimer;
    private final Timer totalTimer;
    private final Timer llmGenerationTimer;

    // === Counters - 计数 ===
    private final Counter totalQueriesCounter;
    private final Counter emptyResultCounter;
    private final Counter rerankFallbackCounter;
    private final Counter permissionDeniedCounter;
    private final Counter retrievalErrorCounter;
    private final Counter llmErrorCounter;

    // === Gauges - 瞬时值 ===
    private final AtomicInteger activeRetrievals = new AtomicInteger(0);

    public RagMetricsCollector(MeterRegistry registry) {
        this.registry = registry;

        this.rewriteTimer = Timer.builder("rag.pipeline.rewrite.time")
                .description("Query rewrite stage latency").register(registry);
        this.hydeTimer = Timer.builder("rag.pipeline.hyde.time")
                .description("HyDE stage latency").register(registry);
        this.retrievalTimer = Timer.builder("rag.pipeline.retrieval.time")
                .description("Vector retrieval stage latency").register(registry);
        this.filterTimer = Timer.builder("rag.pipeline.filter.time")
                .description("Permission filter stage latency").register(registry);
        this.rerankTimer = Timer.builder("rag.pipeline.rerank.time")
                .description("Rerank stage latency").register(registry);
        this.totalTimer = Timer.builder("rag.pipeline.total.time")
                .description("Total pipeline latency").register(registry);
        this.llmGenerationTimer = Timer.builder("rag.llm.generation.time")
                .description("LLM answer generation latency").register(registry);

        this.totalQueriesCounter = Counter.builder("rag.queries.total")
                .description("Total number of RAG queries").register(registry);
        this.emptyResultCounter = Counter.builder("rag.queries.empty")
                .description("Queries returning zero results").register(registry);
        this.rerankFallbackCounter = Counter.builder("rag.rerank.fallback")
                .description("Rerank fallback count").register(registry);
        this.permissionDeniedCounter = Counter.builder("rag.permission.denied")
                .description("Permission filter denials").register(registry);
        this.retrievalErrorCounter = Counter.builder("rag.retrieval.errors")
                .description("Retrieval error count").register(registry);
        this.llmErrorCounter = Counter.builder("rag.llm.errors")
                .description("LLM generation error count").register(registry);

        io.micrometer.core.instrument.Gauge.builder(
                "rag.retrieval.active", activeRetrievals, AtomicInteger::get)
                .description("Active retrieval count").register(registry);
    }

    // === Timer 便捷方法 ===

    public void recordRewrite(long ms) {
        rewriteTimer.record(ms, TimeUnit.MILLISECONDS);
    }

    public void recordHyde(long ms) {
        hydeTimer.record(ms, TimeUnit.MILLISECONDS);
    }

    public void recordRetrieval(long ms) {
        retrievalTimer.record(ms, TimeUnit.MILLISECONDS);
    }

    public void recordFilter(long ms) {
        filterTimer.record(ms, TimeUnit.MILLISECONDS);
    }

    public void recordRerank(long ms) {
        rerankTimer.record(ms, TimeUnit.MILLISECONDS);
    }

    public void recordTotal(long ms) {
        totalTimer.record(ms, TimeUnit.MILLISECONDS);
    }

    public void recordLlmGeneration(long ms) {
        llmGenerationTimer.record(ms, TimeUnit.MILLISECONDS);
    }

    // === Counter 便捷方法 ===

    public void incrementQuery() {
        totalQueriesCounter.increment();
    }

    public void incrementEmptyResult() {
        emptyResultCounter.increment();
    }

    public void incrementRerankFallback() {
        rerankFallbackCounter.increment();
    }

    public void incrementPermissionDenied() {
        permissionDeniedCounter.increment();
    }

    public void incrementRetrievalError() {
        retrievalErrorCounter.increment();
    }

    public void incrementLlmError() {
        llmErrorCounter.increment();
    }

    // === Gauge 便捷方法 ===

    public void incrementActiveRetrievals() {
        activeRetrievals.incrementAndGet();
    }

    public void decrementActiveRetrievals() {
        activeRetrievals.decrementAndGet();
    }
}
