package br.com.repassa.utils;

import java.text.Normalizer;

public class StringUtils {
    public static String normalizerNFD(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD);
    }

    public static String replaceCaracterSpecial(String string) {
        return string.toLowerCase()
                .replaceAll("\\s", "+")
                .replaceAll("[^a-zA-Z0-9+]", "");
    }
}
