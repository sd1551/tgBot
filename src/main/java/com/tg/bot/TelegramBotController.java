package com.tg.bot;

import com.tg.bot.config.BotConfig;
import com.tg.bot.dataPrepair.AnimeTitleMatcher;
import com.tg.bot.dataPrepair.SearchSimilarGenres;
import com.tg.bot.keyboardGenerator.InlineKeyboardBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.*;

import static com.tg.bot.keyboardGenerator.KeyboardBuilder.sendCountOfTitlesButton;

@Component
@EnableScheduling
public class TelegramBotController extends TelegramLongPollingBot {
    private final BotConfig botConfig;
    private final SearchSimilarGenres searchSimilarGenres;
    private final AnimeTitleMatcher animeTitleMatcher;
    private final Set<String> selectedGenres = new HashSet<>();
    private SortingType selectedSortingType = SortingType.NONE;
    private boolean waitingForSorting = false;
    private boolean waitingForTitleCount = false;
    private int count;

    Map<String, String> genres = null;

    public TelegramBotController(BotConfig botConfig, SearchSimilarGenres searchSimilarGenres, AnimeTitleMatcher animeTitleMatcher) {
        this.botConfig = botConfig;
        this.searchSimilarGenres = searchSimilarGenres;
        this.animeTitleMatcher = animeTitleMatcher;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage().getText(), update.getMessage().getChatId());
        } else if (update.hasCallbackQuery()) {
            handleSortCallbackQuery(update.getCallbackQuery());
        }
    }

    @Scheduled(fixedDelay = 2000)
    public void printSelectedValues() {
        System.out.println("Выбранные жанры: " + selectedGenres);
        System.out.println("Метод сортировки: " + selectedSortingType);
        System.out.println("Количество тайтлов: " + count);
    }

    private void handleMessage(String messageText, long chatId) {
        if (messageText.equals("/start")) {
            helloUser(chatId);
        } else if (messageText.equals("/genres")) {
            handleGenresCommand(chatId);
        } else if (waitingForSorting) {
            handleSortingInput(messageText, chatId);
        } else if (waitingForTitleCount) {
            handleTitleCountInput(messageText, chatId);
        } else if (messageText.equals("Найти")) {
            handleSearch(chatId);
        }
    }

    private void handleSortCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();

        if (callbackData.equals("Выбрать тип сортировки")) {
            sendSortingOptions(chatId);
        } else if (callbackData.equals("Сортировка по рейтингу") || callbackData.equals("Сортировка по дате")) {
            handleSortingTypeSelection(callbackData, chatId);
        } else {
            handleGenreSelection(callbackData, chatId);
        }
    }

    private void helloUser(long chatId) {
        sendGenresCommand(chatId);
    }

    private void handleGenresCommand(long chatId) {
        try {
            genres = searchSimilarGenres.getGenreIntersection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sendGenresKeyboard(chatId, genres);
    }

    private void handleSortingInput(String messageText, long chatId) {
        if (messageText.equals("Выбрать тип сортировки")) {
            sendSortingOptions(chatId);
        } else {
            try {
                count = Integer.parseInt(messageText);
                System.out.println("Пользователь ввел количество тайтлов: " + count);
                waitingForTitleCount = false;
                waitingForSorting = false;
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Пожалуйста, введите число.");
            }
        }
    }

    private void handleTitleCountInput(String messageText, long chatId) {
        try {
            count = Integer.parseInt(messageText);
            System.out.println("Пользователь ввел количество тайтлов: " + count);
            waitingForTitleCount = false;
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Пожалуйста, введите число.");
        }
    }

    private void handleSearch(long chatId) {
        if (selectedGenres.isEmpty() || count <= 0 || selectedSortingType == SortingType.NONE) {
            sendMessage(chatId, "Пожалуйста, выберите жанры, укажите количество тайтлов и метод сортировки.");
        } else {
            List<Map<String, String>> similarAnime = searchSimilarAnime();
            sendSimilarAnime(chatId, similarAnime);
            waitingForSorting = false;
            waitingForTitleCount = false;
        }
    }

    private void handleSortingTypeSelection(String callbackData, long chatId) {
        waitingForTitleCount = true;
        selectedSortingType = callbackData.equals("Сортировка по рейтингу") ? SortingType.RATING : SortingType.DATE;
        sendMessage(chatId, "Вы выбрали метод сортировки: " + selectedSortingType);
        sendCountOfTitlesButton(chatId, this);
    }

    private void handleGenreSelection(String callbackData, long chatId) {
        for (Map.Entry<String, String> entry : genres.entrySet()) {
            if (entry.getValue().equals(callbackData)) {
                selectedGenres.clear();
                String foundKey = entry.getKey();
                selectedGenres.add(foundKey);
                break;
            }
        }
        sendMessage(chatId, "Выбран жанр: " + callbackData);
    }

    private List<Map<String, String>> searchSimilarAnime() {
        AnimeTitleMatcher animeTitleMatcher = new AnimeTitleMatcher();
        // Вызываем метод для поиска аниме с пересекающимися названиями
        return animeTitleMatcher.getSimilarTitleAnimeFromScrapers(0.5, count, selectedSortingType.name(), new ArrayList<>(selectedGenres));
    }

    private void sendSimilarAnime(long chatId, List<Map<String, String>> similarAnime) {
        StringBuilder message = new StringBuilder("Аниме с пересекающимися названиями:\n\n");
        for (Map<String, String> anime : similarAnime) {
            message.append("Название: ").append(anime.get("Title")).append("\n");
            message.append("Жанры: ").append(anime.get("Genres")).append("\n");
            message.append("Описание: ").append(anime.get("Description")).append("\n\n");
        }
        sendMessageInParts(chatId, message.toString());
    }

    private void sendMessageInParts(long chatId, String message) {
        int messageLength = message.length();
        final int maxMessageLength = 4096;

        if (messageLength <= maxMessageLength) {
            sendMessage(chatId, message);
        } else {
            int numParts = (int) Math.ceil((double) messageLength / maxMessageLength);
            for (int i = 0; i < numParts; i++) {
                int startIndex = i * maxMessageLength;
                int endIndex = Math.min((i + 1) * maxMessageLength, messageLength);
                String part = message.substring(startIndex, endIndex);
                sendMessage(chatId, part);
            }
        }
    }

    private void sendMessage(long chatId, String messageToSend) {
        MessageSender.sendMessage(chatId, messageToSend, this);
    }

    private void sendGenresCommand(long chatId) {
        MessageSender.sendMessage(chatId, "/genres", this);
    }

    private void sendGenresKeyboard(long chatId, Map<String, String> genres) {
        List<List<String>> keyboard = new ArrayList<>();
        List<String> row = new ArrayList<>();
        for (String value : genres.values()) {
            row.add(value);
            if (row.size() == 2) {
                keyboard.add(new ArrayList<>(row));
                row.clear();
            }
        }
        if (!row.isEmpty()) {
            keyboard.add(row);
        }

        List<String> nextButtonRow = new ArrayList<>();
        nextButtonRow.add("Выбрать тип сортировки");
        keyboard.add(nextButtonRow);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Выберите жанры:");
        sendMessage.setReplyMarkup(InlineKeyboardBuilder.buildInlineKeyboard(keyboard));

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendSortingOptions(long chatId) {
        List<List<String>> keyboard = new ArrayList<>();
        List<String> row1 = new ArrayList<>();
        row1.add("Сортировка по рейтингу");
        row1.add("Сортировка по дате");
        keyboard.add(row1);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Выберите метод сортировки:");
        sendMessage.setReplyMarkup(InlineKeyboardBuilder.buildInlineKeyboard(keyboard));

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}