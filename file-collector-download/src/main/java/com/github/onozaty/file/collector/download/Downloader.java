package com.github.onozaty.file.collector.download;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * ダウンロード処理です。
 * @author onozaty
 */
@Slf4j
public class Downloader {

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .build();

    /**
     * ファイル名として利用できない文字の正規表現
     */
    private static final Pattern INVALID_FILENAME_CHARS_PATTERN = Pattern.compile("[\\/:\\*\\?\"<>|]");

    /**
     * 出力するファイル名の上限
     */
    private static final int OUTPUT_FILE_NAME_LIMIT = 40;

    /**
     * メイン
     * @param args 引数
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption(
                Option.builder("u")
                        .longOpt("urls")
                        .desc("URL list file")
                        .hasArg()
                        .argName("file")
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
            Path urlListFilePath = Paths.get(line.getOptionValue("u"));

            List<String> urls = Files.readAllLines(urlListFilePath);

            log.info("Download started. The number of URLs is {}.", urls.size());

            List<DownloadResult> downloadResults =
                    new Downloader().download(urls, outputBaseDirectoryPath.resolve("files"));

            // ダウンロード結果を出力
            try (DownloadResultWriter writer =
                    new DownloadResultWriter(outputBaseDirectoryPath.resolve("download-results.csv"))) {
                writer.write(downloadResults);
            }

            log.info(
                    "Download finished. The number of files successfully downloaded was {}.",
                    downloadResults.stream().filter(DownloadResult::isSuccess).count());

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
        help.printHelp("java -jar file-collector-download-all.jar", options, true);
        System.exit(1);
    }

    /**
     * ダウンロードします。
     * @param urls 対象URL一覧
     * @param outputDirectoryPath 出力ディレクトリ
     * @return ダウンロード結果
     * @throws IOException
     */
    public List<DownloadResult> download(List<String> urls, Path outputDirectoryPath) throws IOException {

        if (Files.notExists(outputDirectoryPath)) {
            Files.createDirectories(outputDirectoryPath);
        }

        return IntStream.range(0, urls.size())
                .parallel() // 並列でダウンロード
                .mapToObj(index -> {

                    String url = urls.get(index);

                    // 通番は1から
                    int sequence = index + 1;

                    try {
                        Path outputFilePath = download(url, sequence, outputDirectoryPath);
                        return DownloadResult.success(url, outputFilePath);

                    } catch (Exception e) {
                        return DownloadResult.failure(url, e);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * ダウンロードします。
     * @param url URL
     * @param sequence 通番
     * @param outputDirectoryPath 出力フォルダ
     * @return ダウロード先ファイルパス
     * @throws IOException
     */
    private Path download(String url, int sequence, Path outputDirectoryPath) throws IOException {

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                throw new IOException("Request failed. " + response);
            }

            Path outputFilePath = createOutputFilePath(response, sequence, outputDirectoryPath);

            InputStream downloadFileStream = response.body().byteStream();
            try {
                Files.copy(downloadFileStream, outputFilePath);
            } catch (IOException e) {
                try {
                    //  ダウンロード途中のファイルが残っている場合を考慮して削除
                    Files.deleteIfExists(outputFilePath);
                } catch (IOException e1) {
                    e.addSuppressed(e1);
                }

                throw e;
            }

            return outputFilePath;
        }
    }

    /**
     * 出力ファイルのパスを生成します。
     * @param response レスポンス
     * @param sequence 通番
     * @param outputDirectoryPath 出力ディレクトリ
     * @return 出力ファイルのパス
     */
    private Path createOutputFilePath(Response response, int sequence, Path outputDirectoryPath) {

        try {

            String filename = FilenameRetriever.retrieveByContentDisposition(response.header("Content-Disposition"));
            if (StringUtils.isEmpty(filename)) {
                filename = FilenameRetriever.retrieveByUrl(response.request().url().toString());
            }

            if (!StringUtils.isEmpty(filename)) {
                // 保存できない文字は除去
                filename = INVALID_FILENAME_CHARS_PATTERN.matcher(filename).replaceAll("");
            }

            if (StringUtils.isEmpty(filename)) {
                // ファイル名が取得できなかった場合、通番のみのファイル名へ
                return outputDirectoryPath.resolve(String.valueOf(sequence));
            }

            // 長すぎるファイル名となった場合には短縮
            if (filename.length() > OUTPUT_FILE_NAME_LIMIT) {
                // 拡張子は残したいので、先頭部分を消す
                filename = filename.substring(filename.length() - OUTPUT_FILE_NAME_LIMIT);
            }

            // 出力ディレクトリ + 通番_URLのファイル名
            return outputDirectoryPath.resolve(
                    String.format("%d_%s", sequence, filename));

        } catch (Exception e) {
            // ファイル名作成に失敗した場合、通番のみのファイル名へ
            log.warn(String.format("Create filename failed. sequence=[%d]", sequence), e);
            return outputDirectoryPath.resolve(String.valueOf(sequence));
        }
    }
}
