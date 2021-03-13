package com.github.onozaty.file.collector.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.github.onozaty.file.collector.download.DownloadResult;
import com.github.onozaty.file.collector.download.Downloader;

/**
 * {@link Downloader}のテストクラスです。
 * @author onozaty
 */
public class DownloaderTest {

    /**
     * {@link Downloader#download(List, Path)}のテストです。
     * @throws IOException
     */
    @Test
    public void download() throws IOException {

        Downloader downloader = new Downloader();

        Path outputTempDirectoryPath = Files.createTempDirectory(this.getClass().getSimpleName());

        try {
            List<DownloadResult> results = downloader.download(
                    Arrays.asList("https://github.com/onozaty/file-collector/blob/main/README.md"),
                    outputTempDirectoryPath);

            assertThat(results)
                    .containsExactly(
                            DownloadResult.success(
                                    "https://github.com/onozaty/file-collector/blob/main/README.md",
                                    outputTempDirectoryPath.resolve("1_README.md")));

            assertThat(Files.list(outputTempDirectoryPath))
                    .containsExactly(outputTempDirectoryPath.resolve("1_README.md"));

        } finally {
            FileUtils.deleteDirectory(outputTempDirectoryPath.toFile());
        }
    }

    /**
     * {@link Downloader#download(List, Path)}のテストです。
     * @throws IOException
     */
    @Test
    public void download_複数ファイル() throws IOException {

        Downloader downloader = new Downloader();

        Path outputTempDirectoryPath = Files.createTempDirectory(this.getClass().getSimpleName());

        try {
            List<DownloadResult> results = downloader.download(
                    Arrays.asList(
                            "https://github.com/onozaty/file-collector/blob/main/README.md",
                            "https://github.com/onozaty/file-collector/blob/main/gradlew.bat",
                            "https://github.com/onozaty/file-collector/blob/main/file-collector-core/build.gradle"),
                    outputTempDirectoryPath);

            assertThat(results)
                    .containsExactly(
                            DownloadResult.success(
                                    "https://github.com/onozaty/file-collector/blob/main/README.md",
                                    outputTempDirectoryPath.resolve("1_README.md")),
                            DownloadResult.success(
                                    "https://github.com/onozaty/file-collector/blob/main/gradlew.bat",
                                    outputTempDirectoryPath.resolve("2_gradlew.bat")),
                            DownloadResult.success(
                                    "https://github.com/onozaty/file-collector/blob/main/file-collector-core/build.gradle",
                                    outputTempDirectoryPath.resolve("3_build.gradle")));

            assertThat(Files.list(outputTempDirectoryPath))
                    .containsExactlyInAnyOrder(
                            outputTempDirectoryPath.resolve("1_README.md"),
                            outputTempDirectoryPath.resolve("2_gradlew.bat"),
                            outputTempDirectoryPath.resolve("3_build.gradle"));

        } finally {
            FileUtils.deleteDirectory(outputTempDirectoryPath.toFile());
        }
    }

    /**
     * {@link Downloader#download(List, Path)}のテストです。
     * @throws IOException
     */
    @Test
    public void download_失敗あり() throws IOException {

        Downloader downloader = new Downloader();

        Path outputTempDirectoryPath = Files.createTempDirectory(this.getClass().getSimpleName());

        try {
            List<DownloadResult> results = downloader.download(
                    Arrays.asList(
                            "https://github.com/onozaty/file-collector/blob/main/README.md",
                            "https://github.com/onozaty/file-collector/blob/main/xxxxx", // 存在しないURL
                            "https://github.com/onozaty/file-collector/blob/main/file-collector-core/build.gradle"),
                    outputTempDirectoryPath);

            assertThat(results)
                    .extracting(DownloadResult::getUrl, DownloadResult::isSuccess, DownloadResult::getOutputFilePath)
                    .containsExactly(
                            tuple(
                                    "https://github.com/onozaty/file-collector/blob/main/README.md",
                                    true,
                                    outputTempDirectoryPath.resolve("1_README.md")),
                            tuple(
                                    "https://github.com/onozaty/file-collector/blob/main/xxxxx",
                                    false,
                                    null),
                            tuple(
                                    "https://github.com/onozaty/file-collector/blob/main/file-collector-core/build.gradle",
                                    true,
                                    outputTempDirectoryPath.resolve("3_build.gradle")));

            assertThat(Files.list(outputTempDirectoryPath))
                    .containsExactlyInAnyOrder(
                            outputTempDirectoryPath.resolve("1_README.md"),
                            outputTempDirectoryPath.resolve("3_build.gradle"));

        } finally {
            FileUtils.deleteDirectory(outputTempDirectoryPath.toFile());
        }
    }

}
