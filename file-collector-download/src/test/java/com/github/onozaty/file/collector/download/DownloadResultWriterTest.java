package com.github.onozaty.file.collector.download;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * {@link DownloadResultWriter}のテストクラスです。
 * @author onozaty
 */
public class DownloadResultWriterTest {

    /**
     * {@link DownloadResultWriter#write(List)}のテストです。
     * @throws IOException
     */
    @Test
    public void write() throws IOException {

        Path outputTempFilePath = Files.createTempFile(this.getClass().getSimpleName(), null);

        try {

            List<DownloadResult> results = Arrays.asList(
                    DownloadResult.success("http://example.com/1", Paths.get("a/b/success1.txt")),
                    DownloadResult.success("http://example.com/2", Paths.get("a/b/success2.txt")),
                    DownloadResult.failure("http://example.com/3", new Exception()),
                    DownloadResult.success("http://example.com/4", Paths.get("a/b/success4.txt")),
                    DownloadResult.failure("http://example.com/5", new Exception()));

            try (DownloadResultWriter writer = new DownloadResultWriter(outputTempFilePath)) {
                writer.write(results);
            }

            assertThat(outputTempFilePath).hasBinaryContent(
                    ("\uFEFFURL,File Name\r\n"
                            + "http://example.com/1,success1.txt\r\n"
                            + "http://example.com/2,success2.txt\r\n"
                            + "http://example.com/3,\r\n"
                            + "http://example.com/4,success4.txt\r\n"
                            + "http://example.com/5,\r\n")
                                    .getBytes(StandardCharsets.UTF_8));

        } finally {
            Files.delete(outputTempFilePath);
        }
    }

}
