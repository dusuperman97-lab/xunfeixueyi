package com.project.platform.agents.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebContentFetcherTest {

    @Test
    void fetchContent() {
        WebContentFetcher fetcher = new WebContentFetcher();
        String content = fetcher.fetchContent("https://bbs.huaweicloud.com/blogs/468274");
        System.out.println(content);
    }
}
