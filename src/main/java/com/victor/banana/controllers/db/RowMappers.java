package com.victor.banana.controllers.db;

import com.victor.banana.jooq.enums.Notification;
import com.victor.banana.jooq.enums.RoomType;
import com.victor.banana.jooq.enums.State;
import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.desk.Desk;
import com.victor.banana.models.events.locations.Building;
import com.victor.banana.models.events.locations.Company;
import com.victor.banana.models.events.locations.Floor;
import com.victor.banana.models.events.locations.StickyLocation;
import com.victor.banana.models.events.messages.ChatTicketMessage;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.room.Room;
import com.victor.banana.models.events.stickies.Action;
import com.victor.banana.models.events.stickies.ActionState;
import com.victor.banana.models.events.stickies.StickyAction;
import com.victor.banana.models.events.stickies.StickyTitle;
import com.victor.banana.models.events.tickets.NotificationType;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.utils.Constants.PersonnelRole;
import io.vertx.sqlclient.Row;
import org.jetbrains.annotations.NotNull;
import org.jooq.Field;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static com.victor.banana.jooq.Tables.*;

public final class RowMappers {

    private RowMappers() {
    }

    @NotNull
    public static Function<Row, Company> rowToCompany() {
        return r -> Company.builder()
                .id(r.getUUID(COMPANY.COMPANY_ID.getName()))
                .name(r.getString(COMPANY.NAME.getName()))
                .active(r.getBoolean(COMPANY.ACTIVE.getName()))
                .build();
    }

    @NotNull
    public static Function<Row, Building> rowToBuilding() {
        return r -> Building.builder()
                .id(r.getUUID(BUILDING.BUILDING_ID.getName()))
                .companyId(r.getUUID(BUILDING.COMPANY_ID.getName()))
                .name(r.getString(BUILDING.NAME.getName()))
                .active(r.getBoolean(BUILDING.ACTIVE.getName()))
                .build();
    }

    @NotNull
    public static Function<Row, Floor> rowToFloor() {
        return r -> Floor.builder()
                .id(r.getUUID(FLOOR.FLOOR_ID.getName()))
                .buildingId(r.getUUID(FLOOR.BUILDING_ID.getName()))
                .name(r.getString(FLOOR.NAME.getName()))
                .active(r.getBoolean(FLOOR.ACTIVE.getName()))
                .build();
    }

    @NotNull
    public static Function<Row, StickyLocation> rowToStickyLocation() {
        return r -> StickyLocation.builder()
                .id(r.getUUID(STICKY_LOCATION.LOCATION_ID.getName()))
                .floorId(r.getUUID(STICKY_LOCATION.FLOOR_ID.getName()))
                .stickyId(r.getUUID(STICKY_LOCATION.STICKY_ID.getName()))
                .name(r.getString(STICKY_LOCATION.NAME.getName()))
                .active(r.getBoolean(STICKY_LOCATION.ACTIVE.getName()))
                .build();
    }

    @NotNull
    public static Function<Row, Desk> rowToDesk() {
        return r -> Desk.builder()
                .id(r.getUUID(DESK.DESK_ID.getName()))
                .name(r.getString(DESK.NAME.getName()))
                .floorId(r.getUUID(DESK.FLOOR_ID.getName()))
                .active(r.getBoolean(DESK.ACTIVE.getName()))
                .build();
    }

    @NotNull
    public static Function<Row, Room> rowToRoom() {
        return r -> Room.builder()
                .id(r.getUUID(ROOM.ROOM_ID.getName()))
                .name(r.getString(ROOM.NAME.getName()))
                .floorId(r.getUUID(ROOM.FLOOR_ID.getName()))
                .roomType(roomTypeDbToRoomType(RoomType.valueOf(r.getString(ROOM.ROOM_TYPE.getName()))))
                .capacity(r.getInteger(ROOM.CAPACITY.getName()))
                .active(r.getBoolean(ROOM.ACTIVE.getName()))
                .build();
    }

    @NotNull
    public static Function<Row, Ticket> rowToTicket() {
        return r -> Ticket.builder()
                .id(r.getUUID(TICKET.TICKET_ID.getName()))
                .actionId(r.getUUID(TICKET.ACTION_ID.getName()))
                .locationId(r.getUUID(TICKET.LOCATION_ID.getName()))
                .ownedBy(Optional.ofNullable(r.getUUID(TICKET.OWNED_BY.getName())))
                .message(r.getString(TICKET.MESSAGE.getName()))
                .state(ticketToTicketState(State.valueOf(r.getString(TICKET.STATE.getName()))))
                .createdAt(r.getOffsetDateTime(TICKET.CREATED_AT.getName()))
                .acquiredAt(Optional.ofNullable(r.getOffsetDateTime(TICKET.ACQUIRED_AT.getName())))
                .solvedAt(Optional.ofNullable(r.getOffsetDateTime(TICKET.SOLVED_AT.getName())))
                .build();
    }

    @NotNull
    public static Function<Row, Personnel> rowToPersonnel() {
        return r -> Personnel.builder()
                .id(r.getUUID(PERSONNEL.PERSONNEL_ID.getName()))
                .buildingId(Optional.ofNullable(r.getUUID(PERSONNEL.BUILDING_ID.getName())))
                .role(Optional.ofNullable(r.getUUID(PERSONNEL.ROLE_ID.getName())).flatMap(PersonnelRole::from))
                .firstName(Optional.ofNullable(r.getString(PERSONNEL.FIRST_NAME.getName())))
                .lastName(Optional.ofNullable(r.getString(PERSONNEL.LAST_NAME.getName())))
                .email(Optional.ofNullable(r.getString(PERSONNEL.EMAIL.getName())))
                .telegramUsername(Optional.ofNullable(r.getString(TELEGRAM_CHANNEL.USERNAME.getName())))
                .chatId(Optional.ofNullable(r.getLong(TELEGRAM_CHANNEL.CHAT_ID.getName())))
                .build();
    }

    @NotNull
    public static Function<Row, StickyAction> rowToStickyAction() { //todo
        return r -> StickyAction.builder()
                .actionId(r.getUUID(STICKY_ACTION.ACTION_ID.getName()))
                .locationId(r.getUUID(STICKY_LOCATION.LOCATION_ID.getName()))
                .actionMessage(r.getString(STICKY_ACTION.NAME.getName()))
                .floor(r.getString("floor"))
                .location(r.getString("location"))
                .build();
    }

    @NotNull
    public static Function<Row, StickyTitle> rowToStickyTitle() {
        return r -> StickyTitle.builder()
                .id(r.getUUID(STICKY.STICKY_ID.getName()))
                .title(r.getString(STICKY.TITLE.getName()))
                .active(r.getBoolean(STICKY.ACTIVE.getName()))
                .build();
    }

    @NotNull
    public static Function<Row, Action> rowToAction(Field<UUID[]> roles) {
        return r -> Action.builder()
                .id(r.getUUID(STICKY_ACTION.ACTION_ID.getName()))
                .stickyId(r.getUUID(STICKY_ACTION.STICKY_ID.getName()))
                .roles(Arrays.asList(r.getUUIDArray(roles.getName())))
                .name(r.getString(STICKY_ACTION.NAME.getName()))
                .description(Optional.ofNullable(r.getString(STICKY_ACTION.DESCRIPTION.getName())))
                .active(r.getBoolean(STICKY_ACTION.ACTIVE.getName()))
                .state(Optional.ofNullable(r.getUUID(TICKET.TICKET_ID.getName())).map(ignore -> ActionState.IN_PROGRESS).orElse(ActionState.AVAILABLE))
                .build();
    }

    @NotNull
    public static Function<Row, Long> rowToChatId() {
        return r -> r.getLong(TELEGRAM_CHANNEL.CHAT_ID.getName());
    }

    @NotNull
    public static Function<Row, ChatTicketMessage> rowToChatTicketMessage() {
        return r -> ChatTicketMessage.builder()
                .messageId(r.getLong(CHAT_TICKET_MESSAGE.MESSAGE_ID.getName()))
                .chatId(r.getLong(CHAT_TICKET_MESSAGE.CHAT_ID.getName()))
                .ticketId(r.getUUID(CHAT_TICKET_MESSAGE.TICKET_ID.getName()))
                .build();
    }

    @NotNull
    public static Function<Row, TelegramChannel> rowToTelegramChannel() {
        return r -> TelegramChannel.builder()
                .chatId(r.getLong(TELEGRAM_CHANNEL.CHAT_ID.getName()))
                .personnelId(r.getUUID(TELEGRAM_CHANNEL.PERSONNEL_ID.getName()))
                .username(r.getString(TELEGRAM_CHANNEL.USERNAME.getName()))
                .build();
    }

    @NotNull
    public static Notification notificationTypeToNotification(NotificationType ts) {
        return switch (ts) {
            case FOLLOWING -> Notification.FOLLOWING;
            case CREATED_BY -> Notification.CREATED_BY;
        };
    }

    @NotNull
    public static State ticketStateToState(TicketState ts) {
        return switch (ts) {
            case SOLVED -> State.SOLVED;
            case ACQUIRED -> State.ACQUIRED;
            case PENDING -> State.PENDING;
        };
    }

    @NotNull
    public static TicketState ticketToTicketState(State ts) {
        return switch (ts) {
            case SOLVED -> TicketState.SOLVED;
            case ACQUIRED -> TicketState.ACQUIRED;
            case PENDING -> TicketState.PENDING;
        };
    }

    @NotNull
    public static RoomType roomTypeToDbRoomType(com.victor.banana.models.events.room.RoomType ts) {
        return switch (ts) {
            case CONFERENCE_ROOM -> RoomType.CONFERENCE_ROOM;
            case HUB -> RoomType.HUB;
        };
    }

    @NotNull
    public static com.victor.banana.models.events.room.RoomType roomTypeDbToRoomType(RoomType ts) {
        return switch (ts) {
            case CONFERENCE_ROOM -> com.victor.banana.models.events.room.RoomType.CONFERENCE_ROOM;
            case HUB -> com.victor.banana.models.events.room.RoomType.HUB;
        };
    }
}
