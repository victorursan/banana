package com.victor.banana.services;

import com.victor.banana.models.events.ChatTicketMessage;
import com.victor.banana.models.events.StickyAction;
import com.victor.banana.models.events.Ticket;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.List;

import static com.victor.banana.utils.Constants.DeliveryOptionsConstants.LOCAL_DELIVERY;

@ProxyGen
@VertxGen
public interface CartchufiService {

    void stickyActionScanned(StickyAction stickyAction, Handler<AsyncResult<Ticket>> result);

    void messageAcquired(Long chatId, Long messageId, Handler<AsyncResult<List<ChatTicketMessage>>> result);

    static CartchufiService createProxy(Vertx vertx, String address) {
        return new CartchufiServiceVertxEBProxy(vertx, address, LOCAL_DELIVERY);
    }
}
