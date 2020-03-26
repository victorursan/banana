package com.victor.banana.services;


import com.victor.banana.models.events.Ticket;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import static com.victor.banana.utils.Constants.DeliveryOptionsConstants.LOCAL_DELIVERY;

@ProxyGen
@VertxGen
public interface TelegramBotService {

    static TelegramBotService createProxy(Vertx vertx, String address) {
        return new TelegramBotServiceVertxEBProxy(vertx, address, LOCAL_DELIVERY);
    }

    void sendTicket(Ticket message, Handler<AsyncResult<Void>> result);
}
