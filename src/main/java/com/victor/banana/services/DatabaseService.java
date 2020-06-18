package com.victor.banana.services;

import com.victor.banana.models.events.ActionSelected;
import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.locations.*;
import com.victor.banana.models.events.messages.ChatMessage;
import com.victor.banana.models.events.messages.ChatTicketMessage;
import com.victor.banana.models.events.messages.SentDeleteMessage;
import com.victor.banana.models.events.messages.SentTicketMessage;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.personnel.PersonnelFilter;
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

import static com.victor.banana.controllers.db.LocationQueryHandler.addBuildingQ;
import static com.victor.banana.controllers.db.LocationQueryHandler.addFloorQ;
import static com.victor.banana.utils.Constants.DeliveryOptionsConstants.LOCAL_DELIVERY;
import static com.victor.banana.utils.Constants.EventbusAddress.DATABASE;

@ProxyGen
@VertxGen
public interface DatabaseService {
    static DatabaseService createProxy(Vertx vertx) {
        return new DatabaseServiceVertxEBProxy(vertx, DATABASE, LOCAL_DELIVERY);
    }

    void healthCheck(Handler<AsyncResult<Void>> result);

    void addPersonnel(Personnel personnel, Handler<AsyncResult<Personnel>> result);

    void getPersonnel(String personnelId, Handler<AsyncResult<Personnel>> result);

    void findPersonnelWithFilter(PersonnelFilter filter, Handler<AsyncResult<List<Personnel>>> result);

    void updatePersonnel(Personnel personnel, Handler<AsyncResult<Personnel>> result);

    void deactivatePersonnel(String personnelId, Handler<AsyncResult<Void>> result);

    void addChat(TelegramChannel chat, Handler<AsyncResult<TelegramChannel>> result);

    void getChat(Long chatId, Handler<AsyncResult<TelegramChannel>> result);

    void getChats(Ticket ticket, Handler<AsyncResult<List<Long>>> result);

    void setCheckedIn(Long chatId, Boolean checkedIn, Handler<AsyncResult<Void>> result);

    void addMessage(ChatMessage chatMessage, Handler<AsyncResult<ChatMessage>> result);

    void addTicketsMessage(List<SentTicketMessage> chatMessages, Handler<AsyncResult<List<SentTicketMessage>>> result);

    void hideTicketsMessage(List<SentDeleteMessage> chatMessages, Handler<AsyncResult<List<SentDeleteMessage>>> result);

    void getTicketMessageForTicket(String ticketId, Handler<AsyncResult<List<ChatTicketMessage>>> result);

    void getTicketsInStateForChat(Long chatId, TicketState state, Handler<AsyncResult<List<Ticket>>> result);

    void getTicketForMessage(Long chatId, Long messageId, Handler<AsyncResult<Ticket>> result);

    void addSticky(Sticky sticky, Handler<AsyncResult<Sticky>> result);

    void getSticky(String stickyId, Handler<AsyncResult<Sticky>> result);

    void getStickies(Handler<AsyncResult<List<Sticky>>> result);

    void updateStickyActions(UpdateStickyAction updates, Handler<AsyncResult<Void>> result);

    void updateStickyLocation(UpdateStickyLocation updates, Handler<AsyncResult<Void>> result);

    void updateStickyTitle(StickyTitle stickyTitle, Handler<AsyncResult<StickyTitle>> result);

    void getScanSticky(String stickyId, Handler<AsyncResult<ScanSticky>> result);

    void getBuildingLocations(String companyId, Handler<AsyncResult<BuildingLocations>> result);

    void getFloorLocations(String buildingId, Handler<AsyncResult<FloorLocations>> result);

    void addCompany(Company company, Handler<AsyncResult<Company>> result);

    void addBuildingFloors(BuildingFloors buildingFloors, Handler<AsyncResult<BuildingFloors>> result);

    void addBuilding(Building building, Handler<AsyncResult<Building>> result);

    void addFloor(Floor floor, Handler<AsyncResult<Floor>> result);

    void getFloorLocation(String locationId, Handler<AsyncResult<Floor>> result);

    void getBuildingLocation(String locationId, Handler<AsyncResult<Building>> result);

    void getCompanyLocation(String locationId, Handler<AsyncResult<Company>> result);

    void getStickyAction(ActionSelected actionSelected, Handler<AsyncResult<StickyAction>> result);

    void addTicket(Ticket ticket, Handler<AsyncResult<Ticket>> result);

    void addTicketNotification(TicketNotification ticketNotification, Handler<AsyncResult<TicketNotification>> result);

    void updateTicket(Ticket ticket, Handler<AsyncResult<Ticket>> result);

    void getActiveTicketForActionSelected(ActionSelected actionSelected, Handler<AsyncResult<Ticket>> result);

    void getTicket(String ticketId, Handler<AsyncResult<Ticket>> result);

    void getTickets(TicketFilter filter, Handler<AsyncResult<List<Ticket>>> result);
}
