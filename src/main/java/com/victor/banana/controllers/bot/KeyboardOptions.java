package com.victor.banana.controllers.bot;

import com.victor.banana.models.events.messages.TicketMessageState;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.victor.banana.controllers.bot.KeyboardOptions.CallbackData.*;


public class KeyboardOptions {
    enum CallbackData {
        ACQUIRE("acquire"),
        BACK_ACQUIRE("unAcquired"),
        SOLVED("resolve"),
        BACK_RESOLVE("unResolve");

        CallbackData(String data) {
            this.data = data;
        }
        private final String data;

        public static Optional<CallbackData> callbackDataFromString(String str) {
            return Arrays.stream(CallbackData.values()).filter(c -> c.data.equalsIgnoreCase(str)).findFirst();
        }
    }

    private static InlineKeyboardButton acquireButton =  new InlineKeyboardButton("Acquire").setCallbackData(ACQUIRE.data);
    private static InlineKeyboardButton resolveButton =  new InlineKeyboardButton("Resolve").setCallbackData(SOLVED.data);
    private static InlineKeyboardButton backButton =  new InlineKeyboardButton("Back").setCallbackData(BACK_ACQUIRE.data);
    private static InlineKeyboardMarkup emptyKeyboard = new InlineKeyboardMarkup();
    private static InlineKeyboardMarkup line1 = new InlineKeyboardMarkup(List.of(List.of(acquireButton)));
    private static InlineKeyboardMarkup line2 = new InlineKeyboardMarkup(List.of(List.of(backButton, resolveButton)));

    public static InlineKeyboardMarkup getKeyboardFor(CallbackData data) {
        return switch (data) {
            case ACQUIRE -> line2;
            case BACK_ACQUIRE -> line1;
            case SOLVED -> emptyKeyboard;
            default -> emptyKeyboard;
        };
    }

    public static InlineKeyboardMarkup getKeyboardFor(TicketMessageState ticketMessageState) {
        return switch (ticketMessageState) {
            case UN_ACQUIRED -> line1;
            case ACQUIRED, UN_SOLVE -> line2;
            case SOLVED, NO_ACTION -> emptyKeyboard;
        };
    }

    public static TicketMessageState getMessageStateForCallback(Optional<CallbackData> dataOpt) {
        return dataOpt.map(data -> switch (data) {
            case ACQUIRE -> TicketMessageState.ACQUIRED;
            case BACK_ACQUIRE -> TicketMessageState.UN_ACQUIRED;
            case SOLVED -> TicketMessageState.SOLVED;
            case BACK_RESOLVE -> TicketMessageState.UN_SOLVE;
        }).orElse(TicketMessageState.NO_ACTION);
    }

    public static TicketMessageState getMessageStateForCallback(String dataStr) {
        return getMessageStateForCallback(callbackDataFromString(dataStr));
    }

    public static Optional<String> getFormatedMessage(CallbackData data, String personnel) {
        return switch (data) {
            case ACQUIRE -> Optional.of("Aquired by @" + personnel);
            case SOLVED -> Optional.of("Resolved by @" + personnel);
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
