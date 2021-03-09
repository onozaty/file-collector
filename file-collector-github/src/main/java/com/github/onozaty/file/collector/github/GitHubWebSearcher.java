package com.github.onozaty.file.collector.github;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * GitHubのWeb上で検索するクラスです。
 * @author onozaty
 */
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
     * @param query 検索クエリ
     * @return URL一覧
     * @throws Exception
     */
    public List<String> search(String query) throws Exception {

        WebDriver driver = new ChromeDriver();

        try {
            List<String> resultUrls = new ArrayList<>();

            driver.get(
                    "https://github.com/search?type=code&q="
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

        } finally {
            driver.quit();
        }
    }

    private void delay() throws InterruptedException {
        // 1分間に30回の制限を考慮して遅らせる
        // https://docs.github.com/ja/rest/reference/search#rate-limit
        Thread.sleep(2000);
    }

    private List<String> collectResultUrls(WebDriver driver) {

        return driver.findElements(By.cssSelector("div.code-list p.full-path > a")).stream()
                .map(x -> {
                    // この時点では、コードを表示するページのURLなので、ダウンロード用のURLに置換
                    return x.getAttribute("href").replaceFirst("/blob/", "/raw/");
                })
                .collect(Collectors.toList());
    }
}
