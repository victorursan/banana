package com.victor.banana.services.impl;

import com.victor.banana.controllers.db.QueryHandler;
import com.victor.banana.models.events.ActionSelected;
import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.desk.Desk;
import com.victor.banana.models.events.desk.DeskFilter;
import com.victor.banana.models.events.locations.*;
import com.victor.banana.models.events.messages.ChatMessage;
import com.victor.banana.models.events.messages.ChatTicketMessage;
import com.victor.banana.models.events.messages.SentDeleteMessage;
import com.victor.banana.models.events.messages.SentTicketMessage;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.personnel.PersonnelFilter;
import com.victor.banana.models.events.room.Room;
import com.victor.banana.models.events.room.RoomFilter;
import com.victor.banana.models.events.stickies.*;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketFilter;
import com.victor.banana.models.events.tickets.TicketNotification;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.services.DatabaseService;
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.pgclient.PgPool;
import org.jetbrains.annotations.NotNull;
import org.jooq.impl.DefaultConfiguration;

import java.util.List;
import java.util.UUID;

import static com.victor.banana.controllers.db.DeskQueryHandler.*;
import static com.victor.banana.controllers.db.LocationQueryHandler.*;
import static com.victor.banana.controllers.db.PersonnelQueryHandler.*;
import static com.victor.banana.controllers.db.QueryExecutorHandler.*;
import static com.victor.banana.controllers.db.RoomQueryHandler.addRoomQ;
import static com.victor.banana.controllers.db.RoomQueryHandler.getRoomsQ;
import static com.victor.banana.controllers.db.RowMappers.ticketStateToState;
import static com.victor.banana.controllers.db.StickyQueryHandler.*;
import static com.victor.banana.controllers.db.TicketQueryHandler.*;
import static org.jooq.SQLDialect.POSTGRES;

public class DatabaseServiceImpl implements DatabaseService {
    @NotNull
    private final QueryHandler<ReactiveClassicGenericQueryExecutor> queryHandler;

    public DatabaseServiceImpl(PgPool client) {
        final var configuration = new DefaultConfiguration();
        configuration.setSQLDialect(POSTGRES);
        final var queryExecutor = new ReactiveClassicGenericQueryExecutor(configuration, client);
        queryHandler = new QueryHandler<>(queryExecutor);
    }

    @Override
    public final void addCompany(Company company, Handler<AsyncResult<Company>> result) {
        queryHandler.run(addCompanyQ(company)).map(company).onComplete(result);
    }

    @Override
    public final void addBuildingFloors(BuildingFloors buildingFloors, Handler<AsyncResult<BuildingFloors>> result) {
        queryHandler.run(addBuildingFloorsQ(buildingFloors)).map(buildingFloors).onComplete(result);
    }

    @Override
    public final void addBuilding(Building building, Handler<AsyncResult<Building>> result) {
        queryHandler.run(addBuildingQ(building)).map(building).onComplete(result);
    }

    @Override
    public final void addFloor(Floor floor, Handler<AsyncResult<Floor>> result) {
        queryHandler.run(addFloorQ(floor)).map(floor).onComplete(result);
    }

    @Override
    public final void healthCheck(Handler<AsyncResult<Void>> result) {
        queryHandler.run(healthCheckQ()).onComplete(result);
    }

    @Override
    public final void addChat(TelegramChannel chat, Handler<AsyncResult<TelegramChannel>> result) {
        queryHandler.run(addChatQ(chat)).map(chat).onComplete(result);
    }

    @Override
    public final void addDesk(Desk desk, Handler<AsyncResult<Desk>> result) {
        queryHandler.run(addDeskQ(desk)).map(desk).onComplete(result);
    }

    @Override
    public final void addRoom(Room room, Handler<AsyncResult<Room>> result) {
        queryHandler.run(addRoomQ(room)).map(room).onComplete(result);
    }

    @Override
    public final void findDesks(DeskFilter filter, Handler<AsyncResult<List<Desk>>> result) {
        queryHandler.run(getDesksQ(filter)).onComplete(result);
    }

    @Override
    public final void findRooms(RoomFilter filter, Handler<AsyncResult<List<Room>>> result) {
        queryHandler.run(getRoomsQ(filter)).onComplete(result);
    }

    @Override
    public final void addPersonnel(Personnel personnel, Handler<AsyncResult<Personnel>> result) {
        queryHandler.run(addPersonnelQ(personnel)).map(personnel).onComplete(result);
    }

    @Override
    public final void getPersonnel(String personnelId, Handler<AsyncResult<Personnel>> result) {
        queryHandler.run(getPersonnelQ(UUID.fromString(personnelId))).onComplete(result);
    }

    @Override
    public final void findPersonnelWithFilter(PersonnelFilter filter, Handler<AsyncResult<List<Personnel>>> result) {
        queryHandler.run(findPersonnelWithFilterQ(filter)).onComplete(result);
    }

    @Override
    public final void updatePersonnel(Personnel personnel, Handler<AsyncResult<Personnel>> result) {
        queryHandler.run(updatePersonnelQ(personnel)).map(personnel).onComplete(result);
    }

    @Override
    public final void deactivatePersonnel(String personnelId, Handler<AsyncResult<Void>> result) {
        queryHandler.run(deactivatePersonnelQ(UUID.fromString(personnelId))).onComplete(result);
    }

    @Override
    public final void getChat(Long chatId, Handler<AsyncResult<TelegramChannel>> result) {
        queryHandler.run(getChatQ(chatId)).onComplete(result);
    }

    @Override
    public final void getChats(Ticket ticket, Handler<AsyncResult<List<Long>>> result) {
        queryHandler.run(getChatsQ(ticket)).onComplete(result);
    }

    @Override
    public final void setCheckedIn(Long chatId, Boolean checkedIn, Handler<AsyncResult<Void>> result) {
        queryHandler.run(setCheckedInQ(chatId, checkedIn)).onComplete(result);
    }

    @Override
    public final void addMessage(ChatMessage chatMessage, Handler<AsyncResult<ChatMessage>> result) {
        queryHandler.run(addMessageQ(chatMessage)).map(chatMessage).onComplete(result);
    }

    @Override
    public final void addTicketsMessage(List<SentTicketMessage> chatMessages, Handler<AsyncResult<List<SentTicketMessage>>> result) {
        queryHandler.run(addTicketsMessageQ(chatMessages)).map(chatMessages).onComplete(result);
    }

    @Override
    public final void hideTicketsMessage(List<SentDeleteMessage> chatMessages, Handler<AsyncResult<List<SentDeleteMessage>>> result) {
        queryHandler.run(hideTicketsMessageQ(chatMessages)).map(chatMessages).onComplete(result);
    }

    @Override
    public final void getTicketMessageForTicket(String ticketId, Handler<AsyncResult<List<ChatTicketMessage>>> result) {
        queryHandler.run(getTicketMessageForTicketQ(UUID.fromString(ticketId))).onComplete(result);
    }

    @Override
    public final void getScanSticky(String stickyLocationId, Handler<AsyncResult<ScanSticky>> result) {
        queryHandler.run(getScanStickyQ(UUID.fromString(stickyLocationId))).onComplete(result);
    }

    @Override
    public final void getBuildingLocations(String companyId, Handler<AsyncResult<BuildingLocations>> result) {
        queryHandler.run(getBuildingLocationsForCompanyQ(UUID.fromString(companyId))).onComplete(result);
    }

    @Override
    public final void getFloorLocations(String buildingId, Handler<AsyncResult<FloorLocations>> result) {
        queryHandler.run(getFloorLocationsForBuildingQ(UUID.fromString(buildingId))).onComplete(result);

    }

    @Override
    public final void getFloors(String buildingId, Handler<AsyncResult<List<Floor>>> result) {
        queryHandler.run(getFloorsForBuildingQ(UUID.fromString(buildingId))).onComplete(result);
    }


    @Override
    public final void getBuildings(String companyId, Handler<AsyncResult<List<Building>>> result) {
        queryHandler.run(getBuildingsForCompanyQ(UUID.fromString(companyId))).onComplete(result);
    }

    @Override
    public final void getBuildingLocation(String buildingId, Handler<AsyncResult<Building>> result) {
        queryHandler.run(getBuildingWithIdQ(UUID.fromString(buildingId))).onComplete(result);
    }

    @Override
    public final void getStickyAction(ActionSelected actionSelected, Handler<AsyncResult<StickyAction>> result) {
        queryHandler.run(getStickyActionQ(actionSelected)).onComplete(result);
    }

    @Override
    public final void addSticky(Sticky sticky, Handler<AsyncResult<Sticky>> result) {
        queryHandler.run(addStickyQ(sticky)).map(sticky).onComplete(result);
    }

    @Override
    public final void getSticky(String stickyIdS, Handler<AsyncResult<Sticky>> result) {
        final var stickyId = UUID.fromString(stickyIdS);
        queryHandler.run(getStickyQ(stickyId)).onComplete(result);
    }

    @Override
    public final void getStickies(Handler<AsyncResult<List<Sticky>>> result) {
        queryHandler.run(getStickiesQ()).onComplete(result);
    }

    @Override
    public final void updateStickyActions(UpdateStickyAction updates, Handler<AsyncResult<Void>> result) {
        queryHandler.run(updateStickyActionsQ(updates)).onComplete(result);
    }

    @Override
    public final void updateStickyLocation(UpdateStickyLocation updates, Handler<AsyncResult<Void>> result) {
        queryHandler.run(updateStickyLocationsQ(updates)).onComplete(result);
    }

    @Override
    public final void getActiveTicketForActionSelected(ActionSelected actionSelected, Handler<AsyncResult<Ticket>> result) {
        queryHandler.run(getActiveTicketForActionSelectedQ(actionSelected)).onComplete(result);
    }

    @Override
    public final void getTicket(String ticketId, Handler<AsyncResult<Ticket>> result) {
        queryHandler.run(getTicketQ(UUID.fromString(ticketId))).onComplete(result);
    }

    @Override
    public final void getTickets(TicketFilter filter, Handler<AsyncResult<List<Ticket>>> result) {
        queryHandler.run(getTicketsQ(filter)).onComplete(result);
    }

    @Override
    public final void getTicketsInStateForChat(Long chatId, TicketState ticketState, Handler<AsyncResult<List<Ticket>>> result) {
        final var state = ticketStateToState(ticketState);
        queryHandler.run(getTicketsInStateForChatQ(chatId, state)).onComplete(result);
    }

    @Override
    public final void getTicketForMessage(Long chatId, Long messageId, Handler<AsyncResult<Ticket>> result) {
        queryHandler.run(getTicketForMessageQ(chatId, messageId)).onComplete(result);
    }

    @Override
    public final void updateStickyTitle(StickyTitle stickyTitle, Handler<AsyncResult<StickyTitle>> result) {
        queryHandler.run(updateStickyTitleQ(stickyTitle)).map(stickyTitle).onComplete(result);
    }

    @Override
    public final void addTicket(Ticket ticket, Handler<AsyncResult<Ticket>> result) {
        queryHandler.run(addTicketQ(ticket)).flatMap(ignore ->  queryHandler.run(getTicketQ(ticket.getId()))).onComplete(result);
    }

    @Override
    public final void addTicketNotification(TicketNotification ticketNotification, Handler<AsyncResult<TicketNotification>> result) {
        queryHandler.run(addTicketNotificationQ(ticketNotification)).map(ticketNotification).onComplete(result);
    }

    @Override
    public final void updateTicket(Ticket ticket, Handler<AsyncResult<Ticket>> result) {
        queryHandler.run(updateTicketQ(ticket)).map(ticket).onComplete(result);
    }
}
