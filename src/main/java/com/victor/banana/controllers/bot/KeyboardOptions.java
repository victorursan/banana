package com.victor.banana.controllers.bot;

import com.victor.banana.models.events.tickets.TicketState;
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

    public static InlineKeyboardMarkup getKeyboardFor(Optional<TicketState> ticketStateOpt) {
        return ticketStateOpt.map(ticketState -> switch (ticketState) {
            case PENDING -> line1;
            case ACQUIRED -> line2;
            case SOLVED -> emptyKeyboard;
        }).orElse(emptyKeyboard);
    }

    public static TicketState getMessageStateForCallback(CallbackData callbackData) {
        return switch (callbackData) {
            case ACQUIRE, BACK_RESOLVE -> TicketState.ACQUIRED;
            case BACK_ACQUIRE -> TicketState.PENDING;
            case SOLVED -> TicketState.SOLVED;
        };
    }

    public static Optional<TicketState> getMessageStateForCallback(String dataStr) {
        return callbackDataFromString(dataStr).map(KeyboardOptions::getMessageStateForCallback);
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
