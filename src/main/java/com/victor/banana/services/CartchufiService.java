package com.victor.banana.services;

import com.victor.banana.models.events.ActionSelected;
import com.victor.banana.models.events.stickies.CreateSticky;
import com.victor.banana.models.events.stickies.Sticky;
import com.victor.banana.models.events.messages.RecvPersonnelMessage;
import com.victor.banana.models.events.messages.RecvUpdateMessage;
import com.victor.banana.models.events.stickies.StickyLocation;
import com.victor.banana.models.events.tickets.Ticket;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import static com.victor.banana.utils.Constants.DeliveryOptionsConstants.LOCAL_DELIVERY;

@ProxyGen
@VertxGen
public interface CartchufiService {

    void createSticky(CreateSticky createSticky, Handler<AsyncResult<Sticky>> result);

    void getStickyLocation(String stickyLocation, Handler<AsyncResult<StickyLocation>> result);

    void getTicket(String ticketId, Handler<AsyncResult<Ticket>> result);

    void actionSelected(ActionSelected actionSelected, Handler<AsyncResult<Ticket>> result);

    void receivedPersonnelMessage(RecvPersonnelMessage chatMessage);

    void receivedMessageUpdate(RecvUpdateMessage updateMessage);

    static CartchufiService createProxy(Vertx vertx, String address) {
        return new CartchufiServiceVertxEBProxy(vertx, address, LOCAL_DELIVERY);
    }
}
