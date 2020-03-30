package com.victor.banana.services;

import com.victor.banana.models.events.*;
import com.victor.banana.models.events.messages.SentTicketMessage;
import com.victor.banana.models.events.tickets.Ticket;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.List;

import static com.victor.banana.utils.Constants.DeliveryOptionsConstants.LOCAL_DELIVERY;

@ProxyGen
@VertxGen
public interface DatabaseService {

    static DatabaseService createProxy(Vertx vertx, String address) {
        return new DatabaseServiceVertxEBProxy(vertx, address, LOCAL_DELIVERY);
    }

    void healthCheck(Handler<AsyncResult<Void>> result);

    void addPersonnel(Personnel personnel, Handler<AsyncResult<Boolean>> result);

    void addChat(TelegramChannel chat, Handler<AsyncResult<Boolean>> result);

    void getChat(Long chatId, Handler<AsyncResult<TelegramChannel>> result);

    void getChats(Handler<AsyncResult<List<Long>>> result);

    void addMessage(ChatMessage chatMessage, Handler<AsyncResult<Boolean>> result);

    void addTicketsMessage(List<SentTicketMessage> chatMessages, Handler<AsyncResult<Boolean>> result);

    void getTicketMessageForTicket(String ticketId, Handler<AsyncResult<List<ChatTicketMessage>>> result);

    void getTicketForMessage(Long chatId, Long messageId, Handler<AsyncResult<Ticket>> result);

    void addSticky(Sticky sticky, Handler<AsyncResult<Boolean>> result);

    void getSticky(String stickyId, Handler<AsyncResult<Sticky>> result);

    void getStickyAction(String actionId, Handler<AsyncResult<StickyAction>> result);

    void addTicket(Ticket ticket, Handler<AsyncResult<Boolean>> result);

    void updateTicket(Ticket ticket, Handler<AsyncResult<Boolean>> result);

    void getTicket(String ticketId, Handler<AsyncResult<Ticket>> result);
}
