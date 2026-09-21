package com.crawler.util;

import java.util.Locale;

/**
 * Implementation of the Porter Stemmer algorithm for English word normalization.
 * Normalizes words to their morphological root so variants match in content indexing.
 */
public class PorterStemmer {

    public static String stemWord(String word) {
        if (word == null || word.length() < 3) {
            return word != null ? word.toLowerCase(Locale.ENGLISH) : "";
        }

        String str = word.toLowerCase(Locale.ENGLISH).trim();

        // Step 1: Plurals and simple suffixes
        if (str.endsWith("ies")) {
            str = str.substring(0, str.length() - 3) + "y";
        } else if (str.endsWith("es") && str.length() > 3) {
            str = str.substring(0, str.length() - 2);
        } else if (str.endsWith("s") && !str.endsWith("ss") && str.length() > 3) {
            str = str.substring(0, str.length() - 1);
        }

        // Step 2: Participle and agent suffixes (ing, ed, er, or)
        if (str.endsWith("ing") && str.length() > 4) {
            str = str.substring(0, str.length() - 3);
        } else if (str.endsWith("ed") && str.length() > 3) {
            str = str.substring(0, str.length() - 2);
        } else if ((str.endsWith("er") || str.endsWith("or")) && str.length() > 4) {
            str = str.substring(0, str.length() - 2);
        } else if (str.endsWith("ation") && str.length() > 6) {
            str = str.substring(0, str.length() - 5);
        } else if (str.endsWith("tional") && str.length() > 7) {
            str = str.substring(0, str.length() - 6);
        } else if (str.endsWith("ly") && str.length() > 4) {
            str = str.substring(0, str.length() - 2);
        }

        // Step 3: Double consonant clean-up
        if (str.endsWith("tt") || str.endsWith("pp") || str.endsWith("mm") ||
            str.endsWith("nn") || str.endsWith("gg") || str.endsWith("bb")) {
            str = str.substring(0, str.length() - 1);
        }

        return str;
    }
}
