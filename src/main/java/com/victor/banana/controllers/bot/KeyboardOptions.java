package com.victor.banana.controllers.bot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.victor.banana.controllers.bot.KeyboardOptions.CallbackData.*;

public class KeyboardOptions {
    public enum CallbackData {
        ACQUIRE("acquire"),
        RESOLVE("resolve"),
        BACK("back");

        CallbackData(String data) {
            this.data = data;
        }
        private final String data;

        public static Optional<CallbackData> callbackDataFromString(String str) {
            return Arrays.stream(CallbackData.values()).filter(c -> c.data.equalsIgnoreCase(str)).findFirst();
        }
    }

    private static InlineKeyboardButton acquireButton =  new InlineKeyboardButton("Acquire").setCallbackData(ACQUIRE.data);
    private static InlineKeyboardButton resolveButton =  new InlineKeyboardButton("Resolve").setCallbackData(RESOLVE.data);
    private static InlineKeyboardButton backButton =  new InlineKeyboardButton("Back").setCallbackData(BACK.data);
    private static InlineKeyboardMarkup emptyKeyboard = new InlineKeyboardMarkup();
    private static InlineKeyboardMarkup line1 = new InlineKeyboardMarkup(List.of(List.of(acquireButton)));
    private static InlineKeyboardMarkup line2 = new InlineKeyboardMarkup(List.of(List.of(backButton, resolveButton)));

    public static InlineKeyboardMarkup getKeyboardFor(CallbackData data) {
        return switch (data) {
            case ACQUIRE -> line2;
            case BACK -> line1;
            case RESOLVE -> emptyKeyboard;
            default -> emptyKeyboard;
        };
    }

    public static Optional<String> getFormatedMessage(CallbackData data, String personnel) {
        return switch (data) {
            case ACQUIRE -> Optional.of("Aquired by @" + personnel);
            case RESOLVE -> Optional.of("Resolved by @" + personnel);
            default -> Optional.empty();
        };
    }

    public static InlineKeyboardMarkup firstKeyboard() {
        return line1;
    }

    public static InlineKeyboardMarkup noKeyboard() {
        return emptyKeyboard;
    }

}
