package com.tg.bot.scrapper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JutsuAnimeScraper {
    public static Map<String, String> getAvailableGenres() throws IOException {
        String url = "https://jutsu.su/anime";

        Document doc = Jsoup.connect(url).get();

        Map<String, String> availableGenres = new HashMap<>();

        Elements genresElements = doc.select("div.anime_ganres_are_here > div.anime_choose_radio_line > span > a");

        String regex = "/anime/([^/]+)";

        Pattern pattern = Pattern.compile(regex);

        for (Element element : genresElements) {
            String link = element.attr("href");
            Matcher matcher = pattern.matcher(link);
            if (matcher.find()) {
                String genreCode = matcher.group(1);
                String genreName = element.text();
                availableGenres.put(genreCode, genreName);
            }
        }
        return availableGenres;
    }

    public static List<Map<String, String>> getAnimeList(List <String> selectedGenres, int count, String orderType) {
        List<Map<String, String>> comedyAnimeList = new ArrayList<>();
        int maxAnimeCount = count;
        int waitTimeSeconds = 10;
        String datePostfix = "order-by-date/";

        WebDriver driver = null;

        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");

            StringBuilder partOfUrl = new StringBuilder();
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(waitTimeSeconds, TimeUnit.SECONDS);

            for(String genre:selectedGenres){
                partOfUrl.append(genre + "-");
            }

            partOfUrl.deleteCharAt(partOfUrl.length() - 1);
            String url = "";
            if (orderType.equals("RATING")) url = "https://jutsu.su/anime/" + partOfUrl + "/";
            else if (orderType.equals("DATE")) url= "https://jutsu.su/anime/" + partOfUrl + "/" + datePostfix;

            System.out.println(url);
            driver.get(url);

            while (true) {
                List<WebElement> animeElements = driver.findElements(By.cssSelector("div.all_anime_global"));

                for (WebElement animeElement : animeElements) {
                    WebElement titleElement = animeElement.findElement(By.cssSelector(".aaname"));
                    String animeTitle = titleElement.getText();
                    WebElement descriptionElement = animeElement.findElement(By.cssSelector(".tooltip_of_the_anime"));
                    String animeDescriptionHtml = descriptionElement.getAttribute("content");
                    String animeDescriptionText = parseDescription(animeDescriptionHtml);

                    List<String> genres = extractGenres(animeDescriptionText);
                    List<String> themes = extractThemes(animeDescriptionText);
                    List<String> years = extractYears(animeDescriptionText);

                    String descRegex = "\\d{4}\\.([^\\d]*)$";

                    Pattern pattern = Pattern.compile(descRegex);

                    Map<String, String> animeInfo = new HashMap<>();
                    animeInfo.put("Title", animeTitle);
                    Matcher matcher = pattern.matcher(animeDescriptionText);

                    if (matcher.find()) {
                        String text = matcher.group(1);
                        animeInfo.put("Description", text);
                    }

                    animeInfo.put("Genres", String.join(", ", genres));
                    animeInfo.put("Themes", String.join(", ", themes));
                    animeInfo.put("Years", String.join(", ", years));
                    comedyAnimeList.add(animeInfo);
                }
                if (comedyAnimeList.size() >= maxAnimeCount) {
                    break;
                }

                WebDriverWait wait = new WebDriverWait(driver, waitTimeSeconds);
                WebElement nextPageLink = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.load_more_anime > a.vnright")));
                if (nextPageLink != null && nextPageLink.isDisplayed()) {
                    nextPageLink.click();
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }

        return comedyAnimeList;
    }

    private static String parseDescription(String animeDescriptionHtml) {
        Document doc = Jsoup.parse(animeDescriptionHtml);
        return doc.text();
    }

    private static List<String> extractGenres(String description) {
        return extractFromPattern(description, "Жанры:\\s*([^\\.]+)\\.");
    }

    private static List<String> extractThemes(String description) {
        return extractFromPattern(description, "Тем(?:а|ы):\\s*([^\\.]+)\\.");
    }

    private static List<String> extractYears(String description) {
        return extractFromPattern(description, "Год(?:ы)? выпуска:\\s*([^\\.]+)\\.");
    }

    private static List<String> extractFromPattern(String input, String pattern) {
        List<String> result = new ArrayList<>();
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(input);
        if (m.find()) {
            String[] items = m.group(1).split(",\\s*");
            for (String item : items) {
                result.add(item.trim());
            }
        }
        return result;
    }
}
