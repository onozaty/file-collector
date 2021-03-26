package com.github.onozaty.file.collector.github;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;

/**
 * GitHubのWeb上で検索するクラスです。
 * @author onozaty
 */
@Slf4j
public class GitHubWebSearcher {

    /**
     * コンストラクタ
     */
    public GitHubWebSearcher() {
        // ChromeDriverのセットアップ
        WebDriverManager.chromedriver().setup();
    }

    /**
     * 指定クエリでGitHubのコード検索を行い、検索結果からURLを取得します。
     * @param queries 検索クエリ一覧
     * @return URL一覧
     * @throws Exception
     */
    public List<String> search(List<String> queries) throws Exception {

        WebDriver driver = new ChromeDriver();

        try {
            List<String> resultUrls = new ArrayList<>();

            for (int i = 0; i < queries.size(); i++) {
                String query = queries.get(i);

                try {
                    log.info("({}/{}) [{}] Started.", i + 1, queries.size(), query);
                    List<String> urls = search(driver, query);
                    log.info("({}/{}) Finished. The number of URLs was {}.", i + 1, queries.size(), urls.size());

                    resultUrls.addAll(urls);
                } catch (Exception e) {
                    log.error("An error occurred in the search.", e);
                }
            }

            // 複数クエリの場合、重複するURLが存在する可能性があるため
            return resultUrls.stream()
                    .distinct()
                    .collect(Collectors.toList());

        } finally {
            driver.quit();
        }
    }

    /**
     * 指定クエリでGitHubのコード検索を行い、検索結果からURLを取得します。
     * @param query 検索クエリ
     * @return URL一覧
     * @throws Exception
     */
    public List<String> search(String query) throws Exception {

        return search(Arrays.asList(query));
    }

    private List<String> search(WebDriver driver, String query) throws Exception {

        List<String> resultUrls = new ArrayList<>();

        driver.get(
                // 対象はCode、SortはRecently indexedで
                "https://github.com/search?type=code&s=indexed&o=desc&q="
                        + URLEncoder.encode(query, StandardCharsets.UTF_8.name()));

        // ログインが済むまで待ち合わせ
        new WebDriverWait(driver, Duration.ofMinutes(2).getSeconds())
                .until(ExpectedConditions.urlContains("https://github.com/search?"));

        delay();
        resultUrls.addAll(collectResultUrls(driver));

        // 次ページのリンクがなくなるまで繰り返し
        while (!driver.findElements(By.cssSelector("a.next_page")).isEmpty()) {

            driver.findElement(By.cssSelector("a.next_page")).click();

            delay();
            resultUrls.addAll(collectResultUrls(driver));
        }

        return resultUrls;
    }

    private void delay() throws InterruptedException {
        // 1分間に30回の制限を考慮して遅らせる
        // https://docs.github.com/ja/rest/reference/search#rate-limit
        Thread.sleep(2000);
    }

    private List<String> collectResultUrls(WebDriver driver) {

        return driver.findElements(By.cssSelector("div.code-list div.f4 > a")).stream()
                .map(x -> {
                    // この時点では、コードを表示するページのURLなので、ダウンロード用のURLに置換
                    return x.getAttribute("href").replaceFirst("/blob/", "/raw/");
                })
                .collect(Collectors.toList());
    }
}
