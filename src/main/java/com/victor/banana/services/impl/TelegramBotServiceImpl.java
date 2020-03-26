package com.victor.banana.services.impl;

import com.victor.banana.controllers.bot.BotController;
import com.victor.banana.models.configs.TelegramBotConfig;
import com.victor.banana.models.events.Ticket;
import com.victor.banana.services.CartchufiService;
import com.victor.banana.services.DatabaseService;
import com.victor.banana.services.TelegramBotService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBotServiceImpl implements TelegramBotService {
    private BotController botController;

    public TelegramBotServiceImpl(TelegramBotConfig config, DatabaseService databaseService, CartchufiService cartchufiService) {
        ApiContextInitializer.init();

        final var botsApi = new TelegramBotsApi();

        try {
            botController = new BotController(config.getBotUsername(), config.getBotToken(), databaseService, cartchufiService);
            botsApi.registerBot(botController);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendTicket(Ticket message, Handler<AsyncResult<Void>> result) {
        result.handle(botController.broadcastMessage(message));
    }
}
