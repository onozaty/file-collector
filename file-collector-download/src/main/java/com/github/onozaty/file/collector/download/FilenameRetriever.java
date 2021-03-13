package com.github.onozaty.file.collector.download;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.codec.DecoderUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * ファイル名を取得するクラスです。
 * @author onozaty
 */
@Slf4j
public class FilenameRetriever {

    /**
     * Content-Disposition からファイル名を取り出す正規表現
     * <pre>
     * Content-Disposition: inline; filename="xxxx.pdf"
     * Content-Disposition: inline; filename='xxxx.pdf'
     * Content-Disposition: inline; filename = xxxx.pdf
     * Content-Disposition: inline; filename=%E3%81%82%E3%81%84%E3%81%86%E3%81%88%E3%81%8A.pdf
     * Content-Disposition: inline; filename="=?utf-8?B?44GC44GE44GG44GI44GKLnBkZg==?="
     * </pre>
     */
    private static final Pattern CONTENT_DISPOSITION_FILENAME_PATTERN =
            Pattern.compile("filename[\\s]*=[\\s]*['\"]?([^'\";]+)['\";]?");

    /**
     * Content-Disposition からファイル名を取り出す正規表現(エンコードの指定あり)
     * <pre>
     * Content-Disposition: inline; filename*=utf-8''%E3%81%82%E3%81%84%E3%81%86%E3%81%88%E3%81%8A.pdf
     * </pre>
     * filename*0* といった連番のパターンには未対応
     */
    private static final Pattern CONTENT_DISPOSITION_FILENAME_WITH_ENCODING_PATTERN =
            Pattern.compile("filename\\*[\\s]*=[\\s]*([^'']+)''([^'\";]+)");

    /**
     * Content-Dispositionの値から取得します。
     * @param contentDispositionValue Content-Dispositionの値
     * @return ファイル名
     */
    public static String retrieveByContentDisposition(String contentDispositionValue) {

        if (StringUtils.isEmpty(contentDispositionValue)) {
            return null;
        }

        {
            Matcher matcher = CONTENT_DISPOSITION_FILENAME_PATTERN.matcher(contentDispositionValue);
            if (matcher.find()) {
                String value = matcher.group(1);
                String filename;
                try {
                    filename = DecoderUtil.decodeEncodedWords(value, DecodeMonitor.STRICT);
                } catch (Throwable e) { // DecoderUtil#decodeEncodedWordsがErrorを返すことがあるので
                    log.warn(String.format("DecoderUtil#decodeEncodedWords failed. value=[%s]", value), e);
                    filename = value;
                }

                try {
                    return URLDecoder.decode(filename, StandardCharsets.UTF_8.name());
                } catch (Exception e) {
                    log.warn(String.format("URLDecoder#decode failed. value=[%s]", value), e);
                    return filename;
                }
            }
        }

        {
            Matcher matcher = CONTENT_DISPOSITION_FILENAME_WITH_ENCODING_PATTERN.matcher(contentDispositionValue);
            if (matcher.find()) {
                String encoding = matcher.group(1);
                String value = matcher.group(2);

                try {
                    return new URLCodec(encoding).decode(value);
                } catch (Exception e) {
                    // おかしなエンコーディングの場合、デコード前の文字を返却
                    log.warn(
                            String.format("URLCodec#decode failed. encoding=[%s] value=[%s]", encoding, value),
                            e);

                    return value;
                }
            }
        }

        return null;
    }

    /**
     * URLから取得します。
     * @param url URL
     * @return ファイル名
     */
    public static String retrieveByUrl(String url) {

        try {

            // クエリパラメータ部分を除外して、最後の"/"以降を取り出し
            String path = new URL(url).getPath();
            int lastSeparatorIndex = path.lastIndexOf("/");
            String filename = path.substring(lastSeparatorIndex + 1);

            // セミコロンでパラメータ渡しているものがあった場合は、その部分を除外
            int semicolonIndex = filename.indexOf(";");
            if (semicolonIndex != -1) {
                filename = filename.substring(0, semicolonIndex);
            }

            return URLDecoder.decode(filename, StandardCharsets.UTF_8.name());

        } catch (Exception e) {
            log.warn(String.format("retrieveByUrl failed. url=[%s]", url), e);
            return null;
        }
    }
}
