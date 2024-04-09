package com.tg.bot.scrapper;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.util.*;

public class AnimeGoScraper {
    public static Map<String, String> getAvailableGenres() throws IOException {
        String url = "https://animego.org/anime";

        Document doc = Jsoup.connect(url).get();

        Map<String, String> availableGenres = new HashMap<>();

        Elements genresElements = doc.select("input[type=checkbox][data-text]");

        for (Element element : genresElements) {
            String genreName = element.attr("data-text").replaceAll("!", "");
            String genreCode = element.val().replaceAll("!", "");
            availableGenres.put(genreName, genreCode);
        }

        System.out.println("Available genres: " + availableGenres);

        return availableGenres;
    }

    public static List<Map<String, String>> getAnimeList(List<String> selectedGenres, int count, String orderType) {
        List<Map<String, String>> animeList = new ArrayList<>();
        Set<String> uniqueTitles = new HashSet<>();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");

        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver(options);
        String RateOrderPostfix = "?sort=rating&direction=desc";
        String DateOrderPostfix = "?sort=startDate&direction=desc";


        try {
            StringBuilder partOfUrl = new StringBuilder();

            for (String genre : selectedGenres) {
                partOfUrl.append(genre).append("-or-");
            }

            partOfUrl.delete(partOfUrl.length() - 4, partOfUrl.length());
            String url = "";

            if (orderType.equals("RATING")) url = "https://animego.org/anime/filter/genres-is-" + partOfUrl + "/apply" + RateOrderPostfix;
            else if (orderType.equals("DATE")) url = "https://animego.org/anime/filter/genres-is-" + partOfUrl + "/apply" + DateOrderPostfix;

            System.out.println(url);
            driver.get(url);

            while (true) {
                Document doc = Jsoup.parse(driver.getPageSource());
                Elements animeElements = doc.select("div.animes-list-item");

                for (Element animeElement : animeElements) {
                    String title = animeElement.select("div.media-body a").first().ownText();
                    String description = animeElement.select("div.description").text();
                    String genres = animeElement.select("span.anime-genre").text();
                    String year = animeElement.select("span.anime-year a").text();

                    if (!uniqueTitles.contains(title)) {
                        Map<String, String> animeInfo = new HashMap<>();
                        animeInfo.put("Title", title);
                        animeInfo.put("Description", description);
                        animeInfo.put("Genres", genres);
                        animeInfo.put("Year", year);

                        animeList.add(animeInfo);
                        uniqueTitles.add(title);

                        if (animeList.size() >= count) {
                            return animeList;
                        }
                    }
                }

                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("window.scrollBy(0, document.body.scrollHeight)");

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }

        return animeList;
    }
}