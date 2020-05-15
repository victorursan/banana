package com.victor.banana.services.impl;

import com.victor.banana.controllers.bot.BotController;
import com.victor.banana.models.configs.TelegramBotConfig;
import com.victor.banana.models.events.messages.*;
import com.victor.banana.services.CartchufiService;
import com.victor.banana.services.TelegramBotService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

public class TelegramBotServiceImpl implements TelegramBotService {
    private static final Logger log = LoggerFactory.getLogger(TelegramBotServiceImpl.class);
    private BotController botController;

    public TelegramBotServiceImpl(TelegramBotConfig config, CartchufiService cartchufiService) {
        ApiContextInitializer.init();

        final var botsApi = new TelegramBotsApi();

        try {
            botController = new BotController(config.getBotUsername(), config.getBotToken(), cartchufiService);
            botsApi.registerBot(botController);
        } catch (TelegramApiException e) {
            log.error("Something went wrong", e);
        }
    }

    @Override
    public final void sendMessages(List<SendTicketMessage> messages, Handler<AsyncResult<List<SentTicketMessage>>> resultHandler) {
        botController.sendMessages(messages)
                .onComplete(resultHandler);
    }

    @Override
    public final void updateMessages(List<SendUpdateMessage> messages, Handler<AsyncResult<List<SentUpdateMessage>>> resultHandler) {
        botController.updateMessages(messages)
                .onComplete(resultHandler);
    }

    @Override
    public final void deleteMessages(List<SendDeleteMessage> messages, Handler<AsyncResult<List<SentDeleteMessage>>> resultHandler) {
        botController.deleteMessages(messages)
                .onComplete(resultHandler);
    }


}
