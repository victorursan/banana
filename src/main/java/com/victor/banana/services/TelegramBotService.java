package com.victor.banana.services;


import com.victor.banana.models.events.messages.SentTicketMessage;
import com.victor.banana.models.events.messages.SendTicketMessage;
import com.victor.banana.models.events.messages.SendUpdateMessage;
import com.victor.banana.models.events.messages.SentUpdateMessage;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.List;

import static com.victor.banana.utils.Constants.DeliveryOptionsConstants.LOCAL_DELIVERY;

@ProxyGen
@VertxGen
public interface TelegramBotService {

    static TelegramBotService createProxy(Vertx vertx, String address) {
        return new TelegramBotServiceVertxEBProxy(vertx, address, LOCAL_DELIVERY);
    }

    void sendMessages(List<SendTicketMessage> messages, Handler<AsyncResult<List<SentTicketMessage>>> resultHandler);

    void updateMessages(List<SendUpdateMessage> messages, Handler<AsyncResult<List<SentUpdateMessage>>> resultHandler);
}
