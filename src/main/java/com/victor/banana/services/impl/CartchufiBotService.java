package com.victor.banana.services.impl;

import com.victor.banana.controllers.CartchufiBotController;
import com.victor.banana.services.TelegramBotService;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class CartchufiBotService implements TelegramBotService {
    private CartchufiBotController botController;

    public CartchufiBotService() {
        ApiContextInitializer.init();

        final var botsApi = new TelegramBotsApi();

        try {
            botController = new CartchufiBotController();
            botsApi.registerBot(botController);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendMessage(String message) {
        botController.broadcastMessage(message);
    }
}
