package com.victor.banana.services;

import com.victor.banana.models.events.ActionSelected;
import com.victor.banana.models.events.Personnel;
import com.victor.banana.models.events.UpdatePersonnel;
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

    void deleteSticky(String stickyId, Handler<AsyncResult<Boolean>> result);

    void updateSticky(String stickyId, UpdateSticky update, Handler<AsyncResult<Sticky>> result);

    void createLocation(CreateLocation createLocation, Handler<AsyncResult<Location>> result);

    void createRole(CreateRole createRole, Handler<AsyncResult<Role>> result);

    void deleteRole(String roleId, Handler<AsyncResult<Boolean>> result);

    void getStickyLocation(String stickyLocation, Handler<AsyncResult<StickyLocation>> result);

    void getLocations(Handler<AsyncResult<List<Location>>> result);

    void deleteLocation(String locationId, Handler<AsyncResult<Boolean>> result);

    void getRoles(Handler<AsyncResult<List<Role>>> result);

    void getTicket(String ticketId, Handler<AsyncResult<Ticket>> result);

    void getTickets(Handler<AsyncResult<List<Ticket>>> result);

    void requestPersonnelTicketsInState(Long chatId, TicketState state);

    void checkIn(Long chatId);

    void checkOut(Long chatId);

    void actionSelected(ActionSelected actionSelected, Handler<AsyncResult<Ticket>> result);

    void receivedPersonnelMessage(RecvPersonnelMessage chatMessage);

    void receivedMessageUpdate(RecvUpdateMessage updateMessage);

    void getPersonnel(String personnelId, Handler<AsyncResult<Personnel>> result);

    void updatePersonnel(String personnelId, UpdatePersonnel updatePersonnel, Handler<AsyncResult<Personnel>> result);

    static CartchufiService createProxy(Vertx vertx, String address) {
        return new CartchufiServiceVertxEBProxy(vertx, address, LOCAL_DELIVERY);
    }
}
