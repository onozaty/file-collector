package com.github.onozaty.file.collector.github;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * {@link GitHubWebSearcher}のテストクラスです。
 * @author onozaty
 *
 */
public class GitHubWebSearcherTest {

    /**
     * {@link GitHubWebSearcher#search(String)}のテストです。
     * @throws Exception
     */
    @Test
    public void search() throws Exception {

        GitHubWebSearcher searcher = new GitHubWebSearcher();

        List<String> urls = searcher.search("extension:pdf filename:test5");

        assertThat(urls)
                .hasSizeGreaterThan(100);
    }

    /**
     * {@link GitHubWebSearcher#search(List)}のテストです。
     * @throws Exception
     */
    @Test
    public void search_複数クエリ() throws Exception {

        GitHubWebSearcher searcher = new GitHubWebSearcher();

        List<String> urls = searcher.search(
                Arrays.asList("extension:pdf filename:test5", "extension:eml filename:mail"));

        assertThat(urls)
                .hasSizeGreaterThan(500);
    }

}
