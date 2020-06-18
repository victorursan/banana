package com.victor.banana.services;


import com.victor.banana.models.events.messages.*;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.List;

import static com.victor.banana.utils.Constants.DeliveryOptionsConstants.LOCAL_DELIVERY;
import static com.victor.banana.utils.Constants.EventbusAddress.TELEGRAM_BOT;

@ProxyGen
@VertxGen
public interface TelegramBotService {

    static TelegramBotService createProxy(Vertx vertx) {
        return new TelegramBotServiceVertxEBProxy(vertx, TELEGRAM_BOT, LOCAL_DELIVERY);
    }

    void sendMessages(List<SendTicketMessage> messages, Handler<AsyncResult<List<SentTicketMessage>>> resultHandler);

    void updateMessages(List<SendUpdateMessage> messages, Handler<AsyncResult<List<SentUpdateMessage>>> resultHandler);

    void deleteMessages(List<SendDeleteMessage> messages, Handler<AsyncResult<List<SentDeleteMessage>>> resultHandler);
}
