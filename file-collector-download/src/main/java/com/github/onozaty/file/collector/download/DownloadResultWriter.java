package com.github.onozaty.file.collector.download;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * ダウンロード結果をCSVに出力するクラスです。
 * @author onozaty
 */
public class DownloadResultWriter implements Closeable {

    private final CSVPrinter csvPrinter;

    /**
     * コンストラクタ
     * @param outputFilePath 出力先パス
     * @throws IOException
     */
    public DownloadResultWriter(Path outputFilePath) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8);
        csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL);

        writer.write('\uFEFF'); // BOM
        csvPrinter.printRecord("URL", "File Name"); // ヘッダ
    }

    /**
     * 結果を出力します。
     * @param result ダウンロード結果
     * @throws IOException
     */
    public void write(DownloadResult result) throws IOException {

        csvPrinter.printRecord(
                result.getUrl(),
                result.isSuccess()
                        ? result.getOutputFilePath().getFileName()
                        : null);
    }

    /**
     * 結果を出力します。
     * @param results ダウンロード結果一覧
     * @throws IOException
     */
    public void write(List<DownloadResult> results) throws IOException {

        for (DownloadResult result : results) {
            write(result);
        }
    }

    @Override
    public void close() throws IOException {
        csvPrinter.close();
    }

}
