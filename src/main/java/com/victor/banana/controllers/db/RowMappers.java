package com.victor.banana.controllers.db;

import com.victor.banana.jooq.enums.Notification;
import com.victor.banana.jooq.enums.State;
import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.locations.Location;
import com.victor.banana.models.events.messages.ChatTicketMessage;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.roles.Role;
import com.victor.banana.models.events.stickies.Action;
import com.victor.banana.models.events.stickies.ActionState;
import com.victor.banana.models.events.stickies.StickyAction;
import com.victor.banana.models.events.tickets.NotificationType;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketNotification;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.utils.Constants.PersonnelRole;
import io.vertx.core.Future;
import io.vertx.sqlclient.Row;

import java.util.Optional;
import java.util.function.Function;

import static com.victor.banana.jooq.Tables.*;
import static com.victor.banana.utils.Constants.PersonnelRole.NO_ROLE;
import static com.victor.banana.utils.MappersHelper.fToTF;

public final class RowMappers {

    public static Function<Row, Ticket> rowToTicket() {
        return r -> Ticket.builder()
                .id(r.getUUID(TICKET.TICKET_ID.getName()))
                .actionId(r.getUUID(TICKET.ACTION_ID.getName()))
                .locationId(r.getUUID(TICKET.LOCATION_ID.getName()))
                .acquiredBy(Optional.ofNullable(r.getUUID(TICKET.AQUIRED_BY.getName())))
                .solvedBy(Optional.ofNullable(r.getUUID(TICKET.SOLVED_BY.getName())))
                .message(r.getString(TICKET.MESSAGE.getName()))
                .state(ticketToTicketState(State.valueOf(r.getString(TICKET.STATE.getName()))))
                .build();
    }

    public static Function<Row, Personnel> rowToPersonnel() {
        return r -> Personnel.builder()
                .id(r.getUUID(PERSONNEL.PERSONNEL_ID.getName()))
                .locationId(r.getUUID(PERSONNEL.LOCATION_ID.getName()))
                .role(PersonnelRole.from(r.getUUID(PERSONNEL.ROLE_ID.getName())).orElse(NO_ROLE))
                .firstName(Optional.ofNullable(r.getString(PERSONNEL.FIRST_NAME.getName())))
                .lastName(Optional.ofNullable(r.getString(PERSONNEL.LAST_NAME.getName())))
                .email(Optional.ofNullable(r.getString(PERSONNEL.EMAIL.getName())))
                .build();
    }

    public static Function<Row, StickyAction> rowToStickyAction() {
        return r -> StickyAction.builder()
                .actionId(r.getUUID(STICKY_ACTION.ACTION_ID.getName()))
                .locationId(r.getUUID(STICKY_LOCATION.LOCATION_ID.getName()))
                .actionMessage(r.getString("action_message"))
                .stickyMessage(r.getString("sticky_message"))
                .parentLocation(r.getString("parent_location"))
                .location(r.getString("location"))
                .build();
    }

    public static Function<Row, Location> rowToLocation() {
        return r -> Location.builder()
                .id(r.getUUID(LOCATION.LOCATION_ID.getName()))
                .parentLocation(r.getUUID(LOCATION.PARENT_LOCATION.getName()))
                .text(r.getString(LOCATION.MESSAGE.getName()))
                .build();
    }

    public static Function<Row, Action> rowToAction() {
        return r -> Action.builder()
                .id(r.getUUID(STICKY_ACTION.ACTION_ID.getName()))
                .roleId(r.getUUID(STICKY_ACTION.ROLE_ID.getName()))
                .message(r.getString(STICKY_ACTION.MESSAGE.getName()))
                .state(Optional.ofNullable(r.getUUID(TICKET.TICKET_ID.getName())).map(ignore -> ActionState.IN_PROGRESS).orElse(ActionState.AVAILABLE))
                .build();
    }

    public static Function<Row, Role> rowToRole() {
        return r -> Role.builder()
                .id(r.getUUID(ROLE.ROLE_ID.getName()))
                .type(r.getString(ROLE.ROLE_TYPE.getName()))
                .build();
    }

    public static Function<Row, Long> rowToChatId() {
        return r -> r.getLong(TELEGRAM_CHANNEL.CHAT_ID.getName());
    }

    public static Function<Row, ChatTicketMessage> rowToChatTicketMessage() {
        return r -> ChatTicketMessage.builder()
                .messageId(r.getLong(CHAT_TICKET_MESSAGE.MESSAGE_ID.getName()))
                .chatId(r.getLong(CHAT_TICKET_MESSAGE.CHAT_ID.getName()))
                .ticketId(r.getUUID(CHAT_TICKET_MESSAGE.TICKET_ID.getName()))
                .build();
    }

    public static Function<Row, TelegramChannel> rowToTelegramChannel() {
        return r -> TelegramChannel.builder()
                .chatId(r.getLong(TELEGRAM_CHANNEL.CHAT_ID.getName()))
                .personnelId(r.getUUID(TELEGRAM_CHANNEL.PERSONNEL_ID.getName()))
                .username(r.getString(TELEGRAM_CHANNEL.USERNAME.getName()))
                .build();
    }

    public static Function<Row, Future<Ticket>> rowToTicketF() {
        return fToTF(rowToTicket());
    }

    public static Function<Row, Future<Personnel>> rowToPersonnelF() {
        return fToTF(rowToPersonnel());
    }

    public static Function<Row, Future<StickyAction>> rowToStickyActionF() {
        return fToTF(rowToStickyAction());
    }

    public static Function<Row, Future<TelegramChannel>> rowToTelegramChannelF() {
        return fToTF(rowToTelegramChannel());
    }

    public static State ticketStateToState(TicketState ts) {
        return switch (ts) {
            case SOLVED -> State.SOLVED;
            case ACQUIRED -> State.ACQUIRED;
            case PENDING -> State.PENDING;
        };
    }
    public static Notification notificationTypeToNotification(NotificationType ts) {
        return switch (ts) {
            case FOLLOWING -> Notification.FOLLOWING;
            case CREATED_BY -> Notification.CREATED_BY;
        };
    }

    public static TicketState ticketToTicketState(State ts) {
        return switch (ts) {
            case SOLVED -> TicketState.SOLVED;
            case ACQUIRED -> TicketState.ACQUIRED;
            case PENDING -> TicketState.PENDING;
        };
    }

}
