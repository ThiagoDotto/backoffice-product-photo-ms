package br.com.repassa.utils;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    public static String normalizerNFD(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD);
    }

    public static String replaceCaracterSpecial(String string) {
        return string.toLowerCase()
                .replaceAll("\\s", "+")
                .replaceAll("[^a-zA-Z0-9+]", "");
    }

    public static String formatToCloudFrontURL(String s3URL, String cloudFrontURL) {
        return s3URL.replaceAll("https://.*?\\.com", cloudFrontURL);
    }

    public static String extractNumber(String value) {

        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(value);
        StringBuilder numbers = new StringBuilder();
        while (matcher.find()) {
            numbers.append(matcher.group());
        }
        if (!numbers.isEmpty()) {
            return numbers.toString();
        }
        return null;
    }

    public static String replacePlusToBackspace(String url) {
        String[] split = url.split("/");
        String userName = split[1].replaceAll("\\+", " ");
        return url.replace(split[1],userName);
    }
}
