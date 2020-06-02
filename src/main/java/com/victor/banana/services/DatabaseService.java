package com.victor.banana.services;

import com.victor.banana.models.events.*;
import com.victor.banana.models.events.locations.Location;
import com.victor.banana.models.events.messages.ChatMessage;
import com.victor.banana.models.events.messages.ChatTicketMessage;
import com.victor.banana.models.events.messages.SentDeleteMessage;
import com.victor.banana.models.events.messages.SentTicketMessage;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.personnel.PersonnelFilter;
import com.victor.banana.models.events.roles.Role;
import com.victor.banana.models.events.stickies.*;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketFilter;
import com.victor.banana.models.events.tickets.TicketNotification;
import com.victor.banana.models.events.tickets.TicketState;
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

    void getPersonnel(String personnelId, Handler<AsyncResult<Personnel>> result);

    void findPersonnelWithFilter(PersonnelFilter filter, Handler<AsyncResult<List<Personnel>>> result);

    void updatePersonnel(Personnel personnel, Handler<AsyncResult<Boolean>> result);

    void deletePersonnel(String personnelId, Handler<AsyncResult<Boolean>> result);

    void addChat(TelegramChannel chat, Handler<AsyncResult<Boolean>> result);

    void getChat(Long chatId, Handler<AsyncResult<TelegramChannel>> result);

    void getChats(Ticket ticket, Handler<AsyncResult<List<Long>>> result);

    void setCheckedIn(Long chatId, Boolean checkedIn, Handler<AsyncResult<Boolean>> result);

    void ticketsViableForChat(Long chatId, TicketState state, Handler<AsyncResult<List<Ticket>>> result);

    void addMessage(ChatMessage chatMessage, Handler<AsyncResult<Boolean>> result);

    void addTicketsMessage(List<SentTicketMessage> chatMessages, Handler<AsyncResult<Boolean>> result);

    void hideTicketsMessage(List<SentDeleteMessage> chatMessages, Handler<AsyncResult<Boolean>> result);

    void getTicketMessageForTicket(String ticketId, Handler<AsyncResult<List<ChatTicketMessage>>> result);

    void getTicketsInStateForChat(Long chatId, TicketState state, Handler<AsyncResult<List<Ticket>>> result);

    void getTicketForMessage(Long chatId, Long messageId, Handler<AsyncResult<Ticket>> result);

    void addSticky(Sticky sticky, Handler<AsyncResult<Boolean>> result);

    void getSticky(String stickyId, Handler<AsyncResult<Sticky>> result);

    void getStickies(Handler<AsyncResult<List<Sticky>>> result);

    void updateStickyActions(String stickyId, UpdateStickyAction updates, Handler<AsyncResult<Boolean>> result);

    void updateStickyLocation(String stickyId, UpdateStickyLocation updates, Handler<AsyncResult<Boolean>> result);

    void setStickyStatus(StickyStatus stickyStatus, Handler<AsyncResult<Boolean>> result);

    void updateStickyMessage(StickyMessage stickyMessage, Handler<AsyncResult<Boolean>> result);

    void addLocation(Location location, Handler<AsyncResult<Boolean>> result);

    void deactivateLocation(String locationId, Handler<AsyncResult<Boolean>> result);

    void addRole(Role role, Handler<AsyncResult<Boolean>> result);

    void deactivateRole(String roleId, Handler<AsyncResult<Boolean>> result);

    void getStickyLocation(String stickyId, Handler<AsyncResult<StickyLocation>> result);

    void getLocations(Handler<AsyncResult<List<Location>>> result);

    void getLocation(String locationId, Handler<AsyncResult<Location>> result);

    void getStickyAction(ActionSelected actionSelected, Handler<AsyncResult<StickyAction>> result);

    void addTicket(Ticket ticket, Handler<AsyncResult<Boolean>> result);

    void addTicketNotification(TicketNotification ticketNotification, Handler<AsyncResult<Boolean>> result);

    void updateTicket(Ticket ticket, Handler<AsyncResult<Boolean>> result);

    void getActiveTicketForActionSelected(ActionSelected actionSelected, Handler<AsyncResult<Ticket>> result);

    void getTicket(String ticketId, Handler<AsyncResult<Ticket>> result);

    void getTickets(TicketFilter filter, Handler<AsyncResult<List<Ticket>>> result);
}
