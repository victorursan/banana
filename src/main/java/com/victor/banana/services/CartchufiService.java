package com.victor.banana.services;

import com.victor.banana.models.events.ActionSelected;
import com.victor.banana.models.events.TokenUser;
import com.victor.banana.models.events.UserProfile;
import com.victor.banana.models.events.messages.CreateChannelMessage;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.personnel.PersonnelFilter;
import com.victor.banana.models.events.personnel.UpdatePersonnel;
import com.victor.banana.models.events.UpdateTicketState;
import com.victor.banana.models.events.locations.CreateLocation;
import com.victor.banana.models.events.roles.CreateRole;
import com.victor.banana.models.events.locations.Location;
import com.victor.banana.models.events.roles.Role;
import com.victor.banana.models.events.stickies.CreateSticky;
import com.victor.banana.models.events.stickies.Sticky;
import com.victor.banana.models.events.messages.RecvPersonnelMessage;
import com.victor.banana.models.events.messages.RecvUpdateMessage;
import com.victor.banana.models.events.stickies.StickyLocation;
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

@ProxyGen
@VertxGen
public interface CartchufiService {

    void createSticky(CreateSticky createSticky, Handler<AsyncResult<Sticky>> result);

    void updateSticky(String stickyId, UpdateSticky update, Handler<AsyncResult<Sticky>> result);

    void getStickies(Handler<AsyncResult<List<Sticky>>> result);

    void getSticky(String stickyId, Handler<AsyncResult<Sticky>> result);

    void createLocation(CreateLocation createLocation, Handler<AsyncResult<Location>> result);

    void createRole(CreateRole createRole, Handler<AsyncResult<Role>> result);

    void deleteRole(String roleId, Handler<AsyncResult<Boolean>> result);

    void getStickyLocation(String stickyLocation, Handler<AsyncResult<StickyLocation>> result);

    void getLocations(Handler<AsyncResult<List<Location>>> result);

    void deleteLocation(String locationId, Handler<AsyncResult<Boolean>> result);

    void getRoles(Handler<AsyncResult<List<Role>>> result);

    void getUserProfile(Personnel personnel, Handler<AsyncResult<UserProfile>> result);

    void getTicket(String ticketId, Handler<AsyncResult<Ticket>> result);

    void getTickets(TicketFilter ticketFilter, Handler<AsyncResult<List<Ticket>>> result);

    void requestPersonnelTicketsInState(Long chatId, TicketState state);

    void checkIn(Long chatId);

    void checkOut(Long chatId);

    void actionSelected(Personnel personnel, ActionSelected actionSelected, Handler<AsyncResult<Ticket>> result);

    void getOrElseCreatePersonnel(TokenUser user, Handler<AsyncResult<Personnel>> result);

    void receivedPersonnelMessage(RecvPersonnelMessage chatMessage);

    void createTelegramChannel(CreateChannelMessage createChannel, Handler<AsyncResult<Boolean>> result);

    void receivedMessageUpdate(RecvUpdateMessage updateMessage);

    void updateTicketState(String ticketId, UpdateTicketState updateTicketState, Handler<AsyncResult<Ticket>> result);

    void getPersonnel(String personnelId, Handler<AsyncResult<Personnel>> result);

    void findPersonnel(PersonnelFilter filter, Handler<AsyncResult<List<Personnel>>> result);

    void updatePersonnel(String personnelId, UpdatePersonnel updatePersonnel, Handler<AsyncResult<Personnel>> result);

    void deletePersonnel(String personnelId, Handler<AsyncResult<Void>> result);

    static CartchufiService createProxy(Vertx vertx, String address) {
        return new CartchufiServiceVertxEBProxy(vertx, address, LOCAL_DELIVERY);
    }
}
