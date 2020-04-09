package com.victor.banana.controllers.bot;

import com.victor.banana.models.events.messages.TicketMessageState;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.victor.banana.controllers.bot.KeyboardOptions.CallbackData.*;


public final class KeyboardOptions {
    private static final InlineKeyboardButton acquireButton = new InlineKeyboardButton("Acquire").setCallbackData(ACQUIRE.data);
    private static final InlineKeyboardButton resolveButton = new InlineKeyboardButton("Resolve").setCallbackData(SOLVED.data);
    private static final InlineKeyboardButton backButton = new InlineKeyboardButton("Back").setCallbackData(BACK_ACQUIRE.data);
    private static final InlineKeyboardMarkup emptyKeyboard = new InlineKeyboardMarkup();
    private static final InlineKeyboardMarkup line1 = new InlineKeyboardMarkup(List.of(List.of(acquireButton)));
    private static final InlineKeyboardMarkup line2 = new InlineKeyboardMarkup(List.of(List.of(backButton, resolveButton)));

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

    enum CallbackData {
        ACQUIRE("acquire"),
        BACK_ACQUIRE("unAcquired"),
        SOLVED("resolve"),
        BACK_RESOLVE("unResolve");

        private final String data;

        CallbackData(String data) {
            this.data = data;
        }

        public static Optional<CallbackData> callbackDataFromString(String str) {
            return Arrays.stream(CallbackData.values()).filter(c -> c.data.equalsIgnoreCase(str)).findFirst();
        }
    }

}
