package com.github.onozaty.file.collector.download;

import java.nio.file.Path;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * ダウンロード結果です。
 * @author onozaty
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DownloadResult {

    private String url;

    private boolean isSuccess;

    private Path outputFilePath;

    private Exception failedCause;

    public static DownloadResult success(String url, Path outputFilePath) {

        return new DownloadResult(url, true, outputFilePath, null);
    }

    public static DownloadResult failure(String url, Exception failedCause) {

        return new DownloadResult(url, false, null, failedCause);
    }
}
