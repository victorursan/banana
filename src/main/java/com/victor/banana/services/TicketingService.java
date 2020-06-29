package com.victor.banana.services;

import com.victor.banana.models.events.*;
import com.victor.banana.models.events.locations.*;
import com.victor.banana.models.events.messages.CreateChannelMessage;
import com.victor.banana.models.events.messages.RecvPersonnelMessage;
import com.victor.banana.models.events.messages.RecvUpdateMessage;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.personnel.PersonnelFilter;
import com.victor.banana.models.events.personnel.UpdatePersonnel;
import com.victor.banana.models.events.stickies.CreateSticky;
import com.victor.banana.models.events.stickies.ScanSticky;
import com.victor.banana.models.events.stickies.Sticky;
import com.victor.banana.models.events.stickies.UpdateSticky;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketFilter;
import com.victor.banana.models.events.tickets.TicketState;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.List;

import static com.victor.banana.utils.Constants.DeliveryOptionsConstants.LOCAL_DELIVERY;
import static com.victor.banana.utils.Constants.EventbusAddress.TICKETING;

@ProxyGen
@VertxGen
public interface TicketingService { //todo split into ticketing service + personnel service

    static TicketingService createProxy(Vertx vertx) {
        return new TicketingServiceVertxEBProxy(vertx, TICKETING, LOCAL_DELIVERY);
    }

    void createSticky(CreateSticky createSticky, Handler<AsyncResult<Sticky>> result);

    void updateSticky(String stickyId, UpdateSticky update, Handler<AsyncResult<Sticky>> result);

    void getStickies(Handler<AsyncResult<List<Sticky>>> result);

    void getSticky(String stickyId, Handler<AsyncResult<Sticky>> result);

    void getScanSticky(String stickyLocation, Handler<AsyncResult<ScanSticky>> result);

    void getTicket(String ticketId, Handler<AsyncResult<Ticket>> result);

    void getTickets(TicketFilter ticketFilter, Handler<AsyncResult<List<Ticket>>> result);

    void requestPersonnelTicketsInState(Long chatId, TicketState state);

    void checkIn(Long chatId);

    void checkOut(Long chatId);

    void actionSelected(Personnel personnel, ActionSelected actionSelected, Handler<AsyncResult<Ticket>> result);

    void receivedMessageUpdate(RecvUpdateMessage updateMessage);

    void updateTicketState(String ticketId, UpdateTicketState updateTicketState, Handler<AsyncResult<Ticket>> result);
}
