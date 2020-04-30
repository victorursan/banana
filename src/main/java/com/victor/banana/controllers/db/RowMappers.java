package com.victor.banana.controllers.db;

import com.victor.banana.jooq.enums.State;
import com.victor.banana.models.events.Personnel;
import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.locations.Location;
import com.victor.banana.models.events.messages.ChatTicketMessage;
import com.victor.banana.models.events.roles.Role;
import com.victor.banana.models.events.stickies.Action;
import com.victor.banana.models.events.stickies.StickyAction;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketState;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Row;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.victor.banana.jooq.Tables.*;
import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.util.stream.Collectors.toList;

public final class RowMappers {
    private static final Logger log = LoggerFactory.getLogger(RowMappers.class);

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
                .roleId(r.getUUID(PERSONNEL.ROLE_ID.getName()))
                .firstName(r.getString(PERSONNEL.FIRST_NAME.getName()))
                .lastName(r.getString(PERSONNEL.LAST_NAME.getName()))
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
                .build();
    }

    public static Function<Row, Role> rowToRole() {
        return r -> Role.builder()
                .id(r.getUUID(ROLE.ROLE_ID.getName()))
                .type(r.getString(ROLE.ROLE_TYPE.getName()))
                .build();
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

    private static <T> Function<Row, Future<T>> rowToTF(Function<Row, T> mapper) {
        return r -> {
            if (r != null) {
                try {
                    final var ticket = mapper.apply(r);
                    return succeededFuture(ticket);
                } catch (Exception e) {
                    log.error("Failed to map row", e);
                    return failedFuture(e);
                }
            }
            return failedFuture("No Row found");
        };
    }

    private static <T> Function<Row, Stream<T>> rowToTS(Function<Row, T> mapper) {
        return r -> {
            if (r != null) {
                try {
                    final var ticket = mapper.apply(r);
                    return Stream.of(ticket);
                } catch (Exception e) {
                    log.error("Failed to map row", e);
                }
            }
            log.error("No Row found");
            return Stream.empty();
        };
    }

    public static <T> Function<List<Row>, List<T>> mapTs(Function<Row, T> mapper) {
        return rows -> rows.stream()
                .flatMap(rowToTS(mapper))
                .collect(toList());
    }

    public static Function<Row, Future<Ticket>> rowToTicketF() {
        return rowToTF(rowToTicket());
    }

    public static Function<Row, Future<Personnel>> rowToPersonnelF() {
        return rowToTF(rowToPersonnel());
    }

    public static Function<Row, Future<StickyAction>> rowToStickyActionF() {
        return rowToTF(rowToStickyAction());
    }

    public static Function<Row, Future<TelegramChannel>> rowToTelegramChannelF() {
        return rowToTF(rowToTelegramChannel());
    }


    public static State ticketStateToState(TicketState ts) {
        return switch (ts) {
            case SOLVED -> State.SOLVED;
            case ACQUIRED -> State.ACQUIRED;
            case PENDING -> State.PENDING;
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
