package com.tg.bot.dataPrepair;

import com.tg.bot.scrapper.AnimeGoScraper;
import com.tg.bot.scrapper.JutsuAnimeScraper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class AnimeTitleMatcher {

    public List<Map<String, String>> getSimilarTitleAnimeFromScrapers(double similarityThreshold, int count, String orderType, List<String> selectedGenres) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Callable<List<Map<String, String>>>> tasks = new ArrayList<>();

        tasks.add(() -> {
            List<Map<String, String>> jutsuAnimeList = JutsuAnimeScraper.getAnimeList(selectedGenres, count, orderType);
            return jutsuAnimeList;
        });

        tasks.add(() -> {
            List<Map<String, String>> animeGoAnimeList = AnimeGoScraper.getAnimeList(selectedGenres, count, orderType);
            return animeGoAnimeList;
        });

        try {
            List<Future<List<Map<String, String>>>> futures = executor.invokeAll(tasks);
            List<Map<String, String>> jutsuAnimeList = futures.get(0).get();
            List<Map<String, String>> animeGoAnimeList = futures.get(1).get();

            List<Map<String, String>> similarAnime = findSimilarAnime(jutsuAnimeList, animeGoAnimeList, similarityThreshold);
            System.out.println("Аниме с пересекающимися названиями:");
            similarAnime.forEach(anime -> System.out.println("Название: " + anime.get("Title")));

            return similarAnime;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            executor.shutdown();
        }
    }

    private List<Map<String, String>> findSimilarAnime(List<Map<String, String>> list1, List<Map<String, String>> list2, double threshold) {
        List<Map<String, String>> similarAnime = new ArrayList<>();

        for (Map<String, String> anime1 : list1) {
            String title1 = anime1.get("Title").toLowerCase();
            for (Map<String, String> anime2 : list2) {
                String title2 = anime2.get("Title").toLowerCase();
                double similarity = calculateTitleSimilarity(title1, title2);
                if (similarity >= threshold) {
                    similarAnime.add(anime1);
                    break;
                }
            }
        }

        return similarAnime;
    }

    private double calculateTitleSimilarity(String title1, String title2) {
        int maxLength = Math.max(title1.length(), title2.length());
        int commonPrefix = 0;
        for (int i = 0; i < maxLength && i < title1.length() && i < title2.length(); i++) {
            if (title1.charAt(i) != title2.charAt(i)) {
                break;
            }
            commonPrefix++;
        }
        return (double) commonPrefix / maxLength;
    }
}