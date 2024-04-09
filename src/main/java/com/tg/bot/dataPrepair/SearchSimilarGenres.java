package com.tg.bot.dataPrepair;

import com.tg.bot.scrapper.AnimeGoScraper;
import com.tg.bot.scrapper.JutsuAnimeScraper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class SearchSimilarGenres {
    public static Map<String, String> getGenreIntersection() throws IOException {
        Map<String, String> intersection = new HashMap<>();

        Map<String, String> jutsuGenres = JutsuAnimeScraper.getAvailableGenres();
        Map<String, String> animeGoGenres = AnimeGoScraper.getAvailableGenres();

        for (String genre : animeGoGenres.keySet()) {
            String lowerCaseGenre = genre.toLowerCase();

            for (Map.Entry<String, String> entry : jutsuGenres.entrySet()) {
                double similarity = calculateJaccardSimilarity(entry.getValue().toLowerCase(), lowerCaseGenre);
                if (similarity >= 0.5) {
                    intersection.put(entry.getKey(), entry.getValue());
                    break;
                }
            }
        }
        System.out.println(intersection);
        return intersection;
    }

    private static double calculateJaccardSimilarity(String str1, String str2) {
        int intersectionSize = 0;
        int unionSize = 0;
        for (int i = 0; i < str1.length(); i++) {
            if (str2.contains(String.valueOf(str1.charAt(i)))) {
                intersectionSize++;
            }
        }
        unionSize = str1.length() + str2.length() - intersectionSize;
        return (double) intersectionSize / unionSize;
    }
}