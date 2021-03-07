package com.github.onozaty.file.collector.core.download;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * {@link FilenameRetriever}のテストクラスです。
 * 
 * @author onozaty
 */
public class FilenameRetrieverTest {

    /**
     * {@link FilenameRetriever#retrieveByContentDisposition(String)}のテストです。
     */
    @Test
    public void retrieveByContentDisposition() {

        assertThat(FilenameRetriever.retrieveByContentDisposition("inline; filename=\"xxxx.pdf\""))
                .isEqualTo("xxxx.pdf");

        assertThat(FilenameRetriever.retrieveByContentDisposition("inline; filename='xxxx.pdf'"))
                .isEqualTo("xxxx.pdf");

        assertThat(FilenameRetriever.retrieveByContentDisposition("inline; filename = xxxx.pdf"))
                .isEqualTo("xxxx.pdf");

        assertThat(FilenameRetriever
                .retrieveByContentDisposition("inline; filename=%E3%81%82%E3%81%84%E3%81%86%E3%81%88%E3%81%8A.pdf"))
                        .isEqualTo("あいうえお.pdf");

        assertThat(FilenameRetriever
                .retrieveByContentDisposition("inline; filename=\"=?utf-8?B?44GC44GE44GG44GI44GKLnBkZg==?=\""))
                        .isEqualTo("あいうえお.pdf");
    }

    /**
     * {@link FilenameRetriever#retrieveByContentDisposition(String)}のテストです。
     */
    @Test
    public void retrieveByContentDisposition_エンコード指定あり() {

        assertThat(
                FilenameRetriever.retrieveByContentDisposition(
                        "inline; filename*=utf-8''%E3%81%82%E3%81%84%E3%81%86%E3%81%88%E3%81%8A.pdf"))
                                .isEqualTo("あいうえお.pdf");
        assertThat(
                FilenameRetriever.retrieveByContentDisposition(
                        "inline; filename*=shift_jis''%82%A0%82%A2%82%A4%82%A6%82%A8.pdf"))
                                .isEqualTo("あいうえお.pdf");
    }

    /**
     * {@link FilenameRetriever#retrieveByContentDisposition(String)}のテストです。
     */
    @Test
    public void retrieveByContentDisposition_null() {

        assertThat(FilenameRetriever.retrieveByContentDisposition(null))
                .isNull();
    }

    /**
     * {@link FilenameRetriever#retrieveByContentDisposition(String)}のテストです。
     */
    @Test
    public void retrieveByContentDisposition_empty() {

        assertThat(FilenameRetriever.retrieveByContentDisposition(""))
                .isNull();
    }

    /**
     * {@link FilenameRetriever#retrieveByContentDisposition(String)}のテストです。
     */
    @Test
    public void retrieveByContentDisposition_正規表現に一致しない() {

        // = が無い
        assertThat(FilenameRetriever.retrieveByContentDisposition("inline; filename\"xxxx.pdf\""))
                .isNull();
    }

}
