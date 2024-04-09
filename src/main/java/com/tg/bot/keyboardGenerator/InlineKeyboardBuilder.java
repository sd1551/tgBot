package com.tg.bot.keyboardGenerator;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;

public class InlineKeyboardBuilder {
    public static InlineKeyboardMarkup buildInlineKeyboard(List<List<String>> buttons) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        for (List<String> row : buttons) {
            List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
            for (String label : row) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(label);
                button.setCallbackData(label);
                keyboardRow.add(button);
            }
            keyboard.add(keyboardRow);
        }
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
}