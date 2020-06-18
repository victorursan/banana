package com.victor.banana.controllers.db;

import com.victor.banana.jooq.enums.State;
import com.victor.banana.models.events.ActionSelected;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketFilter;
import com.victor.banana.models.events.tickets.TicketNotification;
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor;
import io.vertx.core.Future;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Record9;
import org.jooq.SelectJoinStep;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static com.victor.banana.controllers.db.QueryHandler.*;
import static com.victor.banana.controllers.db.RowMappers.*;
import static com.victor.banana.jooq.Tables.*;

public final class TicketQueryHandler {

    private TicketQueryHandler() {
    }

    @NotNull
    public static SelectJoinStep<Record9<UUID, UUID, UUID, UUID, String, State, OffsetDateTime, OffsetDateTime, OffsetDateTime>> selectTicket(DSLContext c) {
        return c.selectDistinct(TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.LOCATION_ID, TICKET.OWNED_BY, TICKET.MESSAGE, TICKET.STATE, TICKET.CREATED_AT, TICKET.ACQUIRED_AT, TICKET.SOLVED_AT).from(TICKET);
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Ticket>> getActiveTicketForActionSelectedQ(ActionSelected actionSelected) {
        return findOne(c -> selectTicket(c)
                .where(TICKET.ACTION_ID.eq(actionSelected.getActionId()))
                .and(TICKET.LOCATION_ID.eq(actionSelected.getLocationId()))
                .and(TICKET.STATE.in(List.of(State.PENDING, State.ACQUIRED))), rowToTicket());
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Ticket>> getTicketQ(UUID ticketId) {
        return findOne(c -> selectTicket(c)
                .where(TICKET.TICKET_ID.eq(ticketId)), rowToTicket());
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<Ticket>>> getTicketsQ(TicketFilter filter) {
        return t -> filter.getForUser()
                .map(userId ->
                        findMany(c -> selectTicket(c)
                                .innerJoin(PERSONNEL_TICKET).using(TICKET.TICKET_ID)
                                .where(PERSONNEL_TICKET.PERSONNEL_ID.eq(userId)), rowToTicket()).apply(t))
                .orElseGet(
                        () -> findMany(TicketQueryHandler::selectTicket, rowToTicket()).apply(t)
                );
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<Ticket>>> getTicketsInStateForChatQ(Long chatId, State state) {
        return findMany(c ->
                        switch (state) {
                            case PENDING -> selectTicket(c)
                                    .innerJoin(STICKY_LOCATION).using(TICKET.LOCATION_ID)
                                    .innerJoin(FLOOR).using(STICKY_LOCATION.FLOOR_ID)
                                    .innerJoin(PERSONNEL).using(FLOOR.BUILDING_ID)
                                    .innerJoin(TELEGRAM_CHANNEL).using(PERSONNEL.PERSONNEL_ID)
                                    .innerJoin(STICKY_ACTION_ROLE).on(TICKET.ACTION_ID.eq(STICKY_ACTION_ROLE.ACTION_ID).and(PERSONNEL.ROLE_ID.eq(STICKY_ACTION_ROLE.ROLE_ID)))
                                    .where(TICKET.STATE.eq(state).and(TELEGRAM_CHANNEL.CHAT_ID.eq(chatId)));
                            case SOLVED, ACQUIRED -> selectTicket(c)
                                    .innerJoin(PERSONNEL).on(PERSONNEL.PERSONNEL_ID.eq(TICKET.OWNED_BY))
                                    .innerJoin(TELEGRAM_CHANNEL).using(PERSONNEL.PERSONNEL_ID)
                                    .where(TICKET.STATE.eq(state).and(TICKET.OWNED_BY.isNotNull()).and(TELEGRAM_CHANNEL.CHAT_ID.eq(chatId)));
                        }
                , rowToTicket());
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Ticket>> getTicketForMessageQ(Long chatId, Long messageId) {
        return findOne(c -> selectTicket(c)
                .innerJoin(CHAT_TICKET_MESSAGE).using(TICKET.TICKET_ID)
                .where(CHAT_TICKET_MESSAGE.CHAT_ID.eq(chatId).and(CHAT_TICKET_MESSAGE.MESSAGE_ID.eq(messageId))), rowToTicket());
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addTicketQ(Ticket ticket) {
        return execute(c -> c.insertInto(TICKET, TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.LOCATION_ID, TICKET.MESSAGE, TICKET.STATE)
                        .values(ticket.getId(), ticket.getActionId(), ticket.getLocationId(), ticket.getMessage(), ticketStateToState(ticket.getState())),
                1, "add ticket");
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addTicketNotificationQ(TicketNotification ticketNotification) {
        return execute(c -> c.insertInto(PERSONNEL_TICKET, PERSONNEL_TICKET.TICKET_ID, PERSONNEL_TICKET.PERSONNEL_ID, PERSONNEL_TICKET.NOTIFICATION)
                        .values(ticketNotification.getTicketId(), ticketNotification.getPersonnelId(), notificationTypeToNotification(ticketNotification.getType())),
                1, "add ticket notification");
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> updateTicketQ(Ticket ticket) {
        return execute(c ->
                        c.update(TICKET)
                                .set(TICKET.OWNED_BY, ticket.getOwnedBy().orElse(null))
                                .set(TICKET.STATE, ticketStateToState(ticket.getState()))
                                .set(TICKET.ACQUIRED_AT, ticket.getAcquiredAt().orElse(null))
                                .set(TICKET.SOLVED_AT, ticket.getSolvedAt().orElse(null))
                                .where(TICKET.TICKET_ID.eq(ticket.getId())),
                1, "update ticket");
    }


}
