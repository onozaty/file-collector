package com.github.onozaty.file.collector.github;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.github.onozaty.file.collector.download.DownloadResult;
import com.github.onozaty.file.collector.download.Downloader;

import lombok.extern.slf4j.Slf4j;

/**
 * GitHubからファイルを収集するクラスです。
 * @author onozaty
 */
@Slf4j
public class GitHubFileCollector {

    /**
     * メイン
     * @param args 引数
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption(
                Option.builder("q")
                        .longOpt("query")
                        .desc("Search query")
                        .hasArg()
                        .argName("query")
                        .required()
                        .build());
        options.addOption(
                Option.builder("o")
                        .longOpt("output")
                        .desc("Output directory")
                        .hasArg()
                        .argName("directory")
                        .required()
                        .build());

        try {
            CommandLine line = parser.parse(options, args);

            Path outputBaseDirectoryPath = Paths.get(line.getOptionValue("o"));

            List<String> queries = Arrays.asList(line.getOptionValues("q"));

            new GitHubFileCollector().collect(
                    queries,
                    outputBaseDirectoryPath);

        } catch (ParseException e) {
            System.out.println("Unexpected exception:" + e.getMessage());
            System.out.println();

            printUsage(options);
            return;
        }
    }

    private static void printUsage(Options options) {
        HelpFormatter help = new HelpFormatter();
        help.setWidth(200);
        help.setOptionComparator(null); // 順番を変えない

        // ヘルプを出力
        help.printHelp("java -jar file-collector-github-all.jar", options, true);
        System.exit(1);
    }

    /**
     * 収集します。
     * @param queries 検索クエリ一覧
     * @param outputBaseDirectoryPath 出力ディレクトリ
     * @throws Exception
     */
    public void collect(List<String> queries, Path outputBaseDirectoryPath)
            throws Exception {

        if (Files.notExists(outputBaseDirectoryPath)) {
            Files.createDirectories(outputBaseDirectoryPath);
        }

        log.info("Search started. The number of queries is {}.", queries.size());

        List<String> urls = new GitHubWebSearcher().search(queries);

        log.info("Search finished. The number of total URLs is {}.", urls.size());

        // URL一覧を出力
        Files.write(
                outputBaseDirectoryPath.resolve("urls.txt"),
                (urls.stream().collect(Collectors.joining("\n")) + "\n").getBytes(StandardCharsets.UTF_8));

        log.info("Download started.");

        List<DownloadResult> downloadResults =
                new Downloader().download(urls, outputBaseDirectoryPath.resolve("files"));

        log.info(
                "Download finished. The number of files successfully downloaded was {}.",
                downloadResults.stream().filter(DownloadResult::isSuccess).count());

        // ダウンロード結果を出力
        try (BufferedWriter writer = Files.newBufferedWriter(
                outputBaseDirectoryPath.resolve("download-results.csv"), StandardCharsets.UTF_8);
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL)) {

            writer.write('\uFEFF'); // BOM

            csvPrinter.printRecord("URL", "File Name");

            for (DownloadResult result : downloadResults) {
                csvPrinter.printRecord(
                        result.getUrl(),
                        result.isSuccess()
                                ? result.getOutputFilePath().getFileName()
                                : null);
            }
        }
    }
}
