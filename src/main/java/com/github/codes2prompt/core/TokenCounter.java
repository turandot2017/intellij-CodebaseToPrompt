package com.github.codes2prompt.core;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenCounter {

    // 匹配英文单词、数字和常见符号的正则表达式
    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z0-9]+|\\p{Punct}");
    // 匹配中文字符的正则表达式
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");

    /**
     * 估算文本内容的 Tokens 数量
     *
     * @param text 需要计算的文本内容
     * @return 估算的 Tokens 数量
     */
    public static int estimateTokens(@NotNull String text) {
        if (text.isEmpty()) {
            return 0;
        }

        int totalTokens = 0;

        Matcher wordMatcher = WORD_PATTERN.matcher(text);
        Matcher chineseMatcher = CHINESE_PATTERN.matcher(text);

        while (wordMatcher.find()) {
            String match = wordMatcher.group();
            // 英文单词和数字按照长度估算
            if (match.matches("[a-zA-Z0-9]+")) {
                totalTokens += Math.max(1, (int) Math.ceil(match.length() / 4.0));
            } else {
                // 标点符号算作一个 token
                totalTokens++;
            }
        }

        while (chineseMatcher.find()) {
            // 中文字符算作两个 token
            totalTokens += 2;
        }

        return totalTokens;
    }
}
