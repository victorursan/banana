package com.victor.banana.services.impl;

import com.victor.banana.jooq.enums.State;
import com.victor.banana.models.events.*;
import com.victor.banana.models.events.locations.Location;
import com.victor.banana.models.events.messages.ChatMessage;
import com.victor.banana.models.events.messages.ChatTicketMessage;
import com.victor.banana.models.events.messages.SentTicketMessage;
import com.victor.banana.models.events.roles.Role;
import com.victor.banana.models.events.stickies.Action;
import com.victor.banana.models.events.stickies.Sticky;
import com.victor.banana.models.events.stickies.StickyAction;
import com.victor.banana.models.events.stickies.StickyLocation;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.services.DatabaseService;
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import org.jooq.DSLContext;
import org.jooq.impl.DefaultConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.victor.banana.jooq.Tables.*;
import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.util.stream.Collectors.toList;
import static org.jooq.SQLDialect.POSTGRES;

public class DatabaseServiceImpl implements DatabaseService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseServiceImpl.class);

    private final ReactiveClassicGenericQueryExecutor queryExecutor;

    public DatabaseServiceImpl(PgPool client) {
        final var configuration = new DefaultConfiguration();
        configuration.setSQLDialect(POSTGRES);
        queryExecutor = new ReactiveClassicGenericQueryExecutor(configuration, client);
    }

    @Override
    public final void healthCheck(Handler<AsyncResult<Void>> result) {
        queryExecutor.findOneRow(DSLContext::selectOne).<Void>mapEmpty()
                .onComplete(result);
    }

    @Override
    public final void addChat(TelegramChannel chat, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c -> c.insertInto(TELEGRAM_CHANNEL, TELEGRAM_CHANNEL.CHAT_ID, TELEGRAM_CHANNEL.PERSONNEL_ID, TELEGRAM_CHANNEL.USERNAME)
                .values(chat.getChatId(), UUID.fromString(chat.getPersonnelId()), chat.getUsername())
                .onConflictDoNothing()
        ).map(i -> i == 1 || i == 0)
                .onComplete(result);
    }

    @Override
    public final void addPersonnel(Personnel personnel, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c -> c.insertInto(PERSONNEL, PERSONNEL.PERSONNEL_ID, PERSONNEL.FIRST_NAME, PERSONNEL.LAST_NAME, PERSONNEL.LOCATION_ID, PERSONNEL.ROLE_ID)
                .values(UUID.fromString(personnel.getId()), personnel.getFirstName(), personnel.getLastName(), UUID.fromString(personnel.getLocationId()), UUID.fromString(personnel.getRoleId()))
                .onConflictDoNothing()
        ).map(i -> i == 1 || i == 0)
                .onComplete(result);
    }

    @Override
    public final void getChat(Long chatId, Handler<AsyncResult<TelegramChannel>> result) {
        queryExecutor.findOneRow(c -> c.selectFrom(TELEGRAM_CHANNEL)
                .where(TELEGRAM_CHANNEL.CHAT_ID.eq(chatId)))
                .flatMap(r -> {
                    if (r != null) {
                        final var telegramChannel = TelegramChannel.builder()
                                .chatId(r.getLong(TELEGRAM_CHANNEL.CHAT_ID.getName()))
                                .personnelId(r.getUUID(TELEGRAM_CHANNEL.PERSONNEL_ID.getName()).toString())
                                .username(r.getString(TELEGRAM_CHANNEL.USERNAME.getName()))
                                .build();
                        return succeededFuture(telegramChannel);
                    }
                    return failedFuture("No Row found");
                }).onComplete(result);
    }


    @Override
    public final void getChats(Ticket ticket, Handler<AsyncResult<List<Long>>> result) {
        queryExecutor.transaction(t -> {
            final var roleIdRF = t.findOneRow(c -> c.select(STICKY_ACTION.ROLE_ID)
                    .from(STICKY_ACTION)
                    .where(STICKY_ACTION.ACTION_ID.eq(UUID.fromString(ticket.getActionId()))));
            final var locationIdsRF = t.findOneRow(c -> c.select(LOCATION.LOCATION_ID, LOCATION.PARENT_LOCATION).from(LOCATION)
                    .where(LOCATION.LOCATION_ID.eq(UUID.fromString(ticket.getLocationId()))));
            return CompositeFuture.all(roleIdRF, locationIdsRF).flatMap(compositeFuture -> {
                        if (compositeFuture.succeeded()) {
                            return t.findManyRow(c -> {
                                final var rowId = roleIdRF.result().getUUID(STICKY_ACTION.ROLE_ID.getName());
                                final var locationIds = List.of(locationIdsRF.result().getUUID(LOCATION.LOCATION_ID.getName()), locationIdsRF.result().getUUID(LOCATION.PARENT_LOCATION.getName()));
                                return c.selectDistinct(TELEGRAM_CHANNEL.CHAT_ID).from(TELEGRAM_CHANNEL)
                                        .innerJoin(PERSONNEL).using(TELEGRAM_CHANNEL.PERSONNEL_ID)
                                        .where(PERSONNEL.ROLE_ID.eq(rowId)
                                                .and(PERSONNEL.CHECKED_IN.eq(true))
                                                .and(PERSONNEL.LOCATION_ID.in(locationIds)));

                            });
                        } else {
                            return failedFuture(compositeFuture.cause());
                        }
                    }
            ).flatMap(rows -> {
                if (rows != null) {
                    final var ids = rows.stream()
                            .map(r -> r.getLong(TELEGRAM_CHANNEL.CHAT_ID.getName()))
                            .collect(toList());
                    return succeededFuture(ids);
                }
                return failedFuture("No Rows found");
            }).onFailure(e -> t.rollback());
        }).onComplete(result);

    }

    @Override
    public final void setCheckedIn(Long chatId, Boolean checkedIn, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c -> c.update(PERSONNEL).set(PERSONNEL.CHECKED_IN, checkedIn).from(TELEGRAM_CHANNEL)
                .where(PERSONNEL.PERSONNEL_ID.eq(TELEGRAM_CHANNEL.PERSONNEL_ID).and(TELEGRAM_CHANNEL.CHAT_ID.eq(chatId))))
                .map(i -> i == 1)
                .onComplete(result);

    }

    @Override
    public final void ticketsViableForChat(Long chatId, TicketState ticketState, Handler<AsyncResult<List<Ticket>>> result) {//todo
        final var state = ticketStateToState(ticketState);
        queryExecutor.findManyRow(c ->
                c.selectDistinct(TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.LOCATION_ID, TICKET.AQUIRED_BY, TICKET.SOLVED_BY, TICKET.MESSAGE, TICKET.STATE)
                        .from(TICKET)
                        .where(TICKET.STATE.eq(state)))
                .map(t -> t.stream().flatMap(r -> rowToTicket(r).stream()).collect(toList()))
                .onComplete(result);
    }

    @Override
    public final void addMessage(ChatMessage chatMessage, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c -> c.insertInto(CHAT_MESSAGE, CHAT_MESSAGE.MESSAGE_ID, CHAT_MESSAGE.CHAT_ID, CHAT_MESSAGE.MESSAGE)
                .values(chatMessage.getMessageId(), chatMessage.getChatId(), chatMessage.getMessage()))
                .map(i -> i == 1)
                .onComplete(result);
    }

    @Override
    public final void addTicketsMessage(List<SentTicketMessage> chatMessages, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c -> {
            final var insert = c.insertInto(CHAT_TICKET_MESSAGE, CHAT_TICKET_MESSAGE.MESSAGE_ID, CHAT_TICKET_MESSAGE.CHAT_ID, CHAT_TICKET_MESSAGE.TICKET_ID);
            chatMessages.forEach(chatMessage ->
                    insert.values(chatMessage.getMessageId(), chatMessage.getChatId(), UUID.fromString(chatMessage.getTicketId())));
            return insert.onConflictDoNothing();
        })
                .map(i -> i == chatMessages.size())
                .onComplete(result);
    }

    @Override
    public final void getTicketMessageForTicket(String ticketId, Handler<AsyncResult<List<ChatTicketMessage>>> result) {
        queryExecutor.findManyRow(c -> c.select(CHAT_TICKET_MESSAGE.MESSAGE_ID, CHAT_TICKET_MESSAGE.CHAT_ID)
                .from(CHAT_TICKET_MESSAGE)
                .innerJoin(TELEGRAM_CHANNEL).using(TELEGRAM_CHANNEL.CHAT_ID)
                .innerJoin(PERSONNEL).using(TELEGRAM_CHANNEL.PERSONNEL_ID)
                .where(CHAT_TICKET_MESSAGE.TICKET_ID.eq(UUID.fromString(ticketId)).and(PERSONNEL.CHECKED_IN.eq(true))))
                .map(rows -> rows.stream()
                        .map(row ->
                                ChatTicketMessage.builder()
                                        .messageId(row.getLong(CHAT_TICKET_MESSAGE.MESSAGE_ID.getName()))
                                        .chatId(row.getLong(CHAT_TICKET_MESSAGE.CHAT_ID.getName()))
                                        .ticketId(ticketId)
                                        .build()).collect(toList()))
                .onComplete(result);
    }

    @Override
    public final void getTicketsInStateForChat(Long chatId, TicketState ticketState, Handler<AsyncResult<List<Ticket>>> result) {
        final var state = ticketStateToState(ticketState);
        queryExecutor.findManyRow(c ->
                c.selectDistinct(TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.LOCATION_ID, TICKET.AQUIRED_BY, TICKET.SOLVED_BY, TICKET.MESSAGE, TICKET.STATE)
                        .from(TICKET)
                        .innerJoin(CHAT_TICKET_MESSAGE).using(TICKET.TICKET_ID)
                        .innerJoin(TELEGRAM_CHANNEL).on(TELEGRAM_CHANNEL.PERSONNEL_ID.eq(TICKET.AQUIRED_BY))
                        .where(TELEGRAM_CHANNEL.CHAT_ID.eq(chatId)
                                .and(TICKET.STATE.eq(state))))
                .map(t -> t.stream().flatMap(r -> rowToTicket(r).stream()).collect(toList()))
                .onComplete(result);
    }

    @Override
    public final void getTicketForMessage(Long chatId, Long messageId, Handler<AsyncResult<Ticket>> result) {
        queryExecutor.findOneRow(c ->
                c.select(TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.LOCATION_ID, TICKET.AQUIRED_BY, TICKET.SOLVED_BY, TICKET.MESSAGE, TICKET.STATE)
                        .from(CHAT_TICKET_MESSAGE)
                        .innerJoin(TICKET)
                        .using(TICKET.TICKET_ID)
                        .where(CHAT_TICKET_MESSAGE.CHAT_ID.eq(chatId).and(CHAT_TICKET_MESSAGE.MESSAGE_ID.eq(messageId))))
                .flatMap(this::rowToTicketF)
                .onComplete(result);

    }

    @Override
    public final void getStickyLocation(String stickyLocationId, Handler<AsyncResult<StickyLocation>> result) {
        queryExecutor.transaction(t -> {
            final var stickyQ = t.findOneRow(c ->
                    c.select(STICKY.STICKY_ID, STICKY.MESSAGE)
                            .from(STICKY)
                            .innerJoin(STICKY_LOCATION).using(STICKY.STICKY_ID)
                            .where(STICKY_LOCATION.LOCATION_ID.eq(UUID.fromString(stickyLocationId))).and(STICKY.ACTIVE.eq(true)));
            return stickyQ.flatMap(stickyR -> {
                if (stickyR != null) {
                    final var stickyId = stickyR.getUUID(STICKY.STICKY_ID.getName());
                    final var actionsQ = t.findManyRow(c ->
                            c.select(STICKY_ACTION.ACTION_ID, STICKY_ACTION.MESSAGE)
                                    .from(STICKY_ACTION)
                                    .where(STICKY_ACTION.STICKY_ID.eq(stickyId)
                                            .and(STICKY_ACTION.ACTIVE.eq(true))));

                    return actionsQ.flatMap(actionsR -> {
                        if (actionsR != null) {
                            final var sticky = StickyLocation.builder()
                                    .id(stickyId.toString())
                                    .locationId(stickyLocationId)
                                    .message(stickyR.getString(STICKY.MESSAGE.getName()))
                                    .actions(actionsR.stream().map(actionR -> Action.builder()
                                            .id(actionR.getUUID(STICKY_ACTION.ACTION_ID.getName()).toString())
                                            .message(actionR.getString(STICKY_ACTION.MESSAGE.getName()))
                                            .build()).collect(toList()))
                                    .build();
                            return succeededFuture(sticky);
                        }
                        return failedFuture("No Row found");
                    });
                }
                return failedFuture("No Row found");
            }).onFailure(ignore -> t.rollback());
        }).onComplete(result);
    }

    @Override
    public final void getLocations(Handler<AsyncResult<List<Location>>> result) {
        queryExecutor.findManyRow(c -> c.select(LOCATION.LOCATION_ID, LOCATION.PARENT_LOCATION, LOCATION.MESSAGE).from(LOCATION)
                .leftOuterJoin(STICKY_LOCATION).using(LOCATION.LOCATION_ID)
                .where(STICKY_LOCATION.LOCATION_ID.isNull().and(LOCATION.ACTIVE.eq(true))))
                .map(l -> l.stream()
                        .map(r -> Location.builder()
                                .id(r.getUUID(LOCATION.LOCATION_ID.getName()).toString())
                                .parentLocation(r.getUUID(LOCATION.PARENT_LOCATION.getName()).toString())
                                .text(r.getString(LOCATION.MESSAGE.getName()))
                                .build())
                        .collect(toList()))
                .onComplete(result);

    }

    @Override
    public void getRoles(Handler<AsyncResult<List<Role>>> result) {
        queryExecutor.findManyRow(c -> c.select(ROLE.ROLE_ID, ROLE.ROLE_TYPE).from(ROLE).where(ROLE.ACTIVE.eq(true)))
                .map(l -> l.stream()
                        .map(r -> Role.builder()
                                .id(r.getUUID(ROLE.ROLE_ID.getName()).toString())
                                .type(r.getString(ROLE.ROLE_TYPE.getName()))
                                .build())
                        .collect(toList()))
                .onComplete(result);
    }

    @Override
    public final void getStickyAction(ActionSelected actionSelected, Handler<AsyncResult<StickyAction>> result) {
        queryExecutor.findOneRow(c -> c.select(STICKY.MESSAGE.as("sticky_message"), STICKY_ACTION.MESSAGE.as("action_message"), STICKY_LOCATION.MESSAGE.as("location"))
                .from(STICKY_ACTION)
                .innerJoin(STICKY).using(STICKY_ACTION.STICKY_ID)
                .innerJoin(STICKY_LOCATION).using(STICKY.STICKY_ID)
                .where(STICKY_ACTION.ACTION_ID.eq(UUID.fromString(actionSelected.getActionId()))
                        .and(STICKY_LOCATION.LOCATION_ID.eq(UUID.fromString(actionSelected.getLocationId())))))
                .flatMap(r -> {
                    if (r != null) {
                        final var stickyAction = StickyAction.builder()
                                .actionId(actionSelected.getActionId())
                                .locationId(actionSelected.getLocationId())
                                .actionMessage(r.getString("action_message"))
                                .stickyMessage(r.getString("sticky_message"))
                                .location(r.getString("location"))
                                .build();
                        return succeededFuture(stickyAction);
                    }
                    return failedFuture("No Row found");
                })
                .onComplete(result);
    }

    @Override
    public final void addSticky(Sticky sticky, Handler<AsyncResult<Boolean>> result) {
        final var stickyId = UUID.fromString(sticky.getId());
        queryExecutor.transaction(t ->
                t.execute(c -> c.insertInto(STICKY, STICKY.STICKY_ID, STICKY.MESSAGE)
                        .values(stickyId, sticky.getMessage()))
                        .flatMap(i -> {
                            if (i == 1) {
                                final var addActions = t.execute(c -> {
                                            final var insert = c.insertInto(STICKY_ACTION, STICKY_ACTION.ACTION_ID, STICKY_ACTION.STICKY_ID, STICKY_ACTION.ROLE_ID, STICKY_ACTION.MESSAGE);
                                            sticky.getActions().forEach(action -> insert.values(UUID.fromString(action.getId()), stickyId, UUID.fromString(action.getRoleId()), action.getMessage()));
                                            return insert;
                                        }
                                ).map(ii -> ii == sticky.getActions().size());
                                final var addLocations = t.execute(c -> {
                                    final var insert = c.insertInto(LOCATION, LOCATION.LOCATION_ID, LOCATION.PARENT_LOCATION, LOCATION.MESSAGE);
                                    sticky.getLocations().forEach(location -> insert.values(UUID.fromString(location.getId()), UUID.fromString(location.getParentLocation()), location.getText()));
                                    return insert;
                                }).map(ii -> ii == sticky.getLocations().size());

                                final var addStickyLocations = addLocations.flatMap(loc -> {
                                            if (loc) {
                                                return t.execute(c -> {
                                                    final var insert = c.insertInto(STICKY_LOCATION, STICKY_LOCATION.LOCATION_ID, STICKY_LOCATION.STICKY_ID, STICKY_LOCATION.MESSAGE);
                                                    sticky.getLocations().forEach(location -> insert.values(UUID.fromString(location.getId()), stickyId, location.getText()));
                                                    return insert;
                                                });
                                            }
                                            return failedFuture("Failed to insert location");
                                        }

                                ).map(ii -> ii == sticky.getLocations().size());
                                return CompositeFuture.join(addActions, addStickyLocations)
                                        .map(c ->
                                                addActions.result() && addStickyLocations.result());
                            }
                            return failedFuture("Failed to insert sticky");
                        }).onFailure(ignore -> t.rollback()))
                .onComplete(result);
    }

    @Override
    public final void addLocation(Location location, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c ->
                c.insertInto(LOCATION, LOCATION.LOCATION_ID, LOCATION.PARENT_LOCATION, LOCATION.MESSAGE)
                        .values(UUID.fromString(location.getId()), UUID.fromString(location.getParentLocation()), location.getText()))
                .map(i -> i == 1)
                .onComplete(result);
    }

    @Override
    public final void addRole(Role role, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c ->
                c.insertInto(ROLE, ROLE.ROLE_ID, ROLE.ROLE_TYPE)
                        .values(UUID.fromString(role.getId()), role.getType()))
                .map(i -> i == 1)
                .onComplete(result);
    }

    @Override
    public final void getActiveTicketForActionSelected(ActionSelected actionSelected, Handler<AsyncResult<Ticket>> result) {
        queryExecutor.findOneRow(c ->
                c.select(TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.LOCATION_ID, TICKET.AQUIRED_BY, TICKET.SOLVED_BY, TICKET.MESSAGE, TICKET.STATE).from(TICKET)
                        .where(TICKET.ACTION_ID.eq(UUID.fromString(actionSelected.getActionId()))
                                .and(TICKET.LOCATION_ID.eq(UUID.fromString(actionSelected.getLocationId())))
                                .and(TICKET.STATE.in(List.of(State.PENDING, State.ACQUIRED)))))
                .flatMap(this::rowToTicketF)
                .onComplete(result);
    }

    @Override
    public final void getTicket(String ticketId, Handler<AsyncResult<Ticket>> result) {
        queryExecutor.findOneRow(c -> c.select(TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.LOCATION_ID, TICKET.AQUIRED_BY, TICKET.SOLVED_BY, TICKET.MESSAGE, TICKET.STATE).from(TICKET)
                .where(TICKET.TICKET_ID.eq(UUID.fromString(ticketId))))
                .flatMap(this::rowToTicketF)
                .onComplete(result);
    }

    @Override
    public final void deactivateSticky(String stickyIdS, Handler<AsyncResult<Boolean>> result) {
        final var stickyId = UUID.fromString(stickyIdS);
        queryExecutor.transaction(t -> {
            final var stickyActions = t.execute(c -> c.update(STICKY_ACTION).set(STICKY_ACTION.ACTIVE, false).where(STICKY_ACTION.STICKY_ID.eq(stickyId)));
            final var sticky = t.execute(c -> c.update(STICKY).set(STICKY.ACTIVE, false).where(STICKY.STICKY_ID.eq(stickyId)));
            return CompositeFuture.all(stickyActions, sticky).map(true)
                    .otherwise(e -> {
                        log.error("Couldn't deactivate sticky", e);
                        t.rollback();
                        return false;
                    });
        }).onComplete(result);
    }

    @Override
    public final void deactivateLocation(String locationId, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c -> c.update(LOCATION).set(LOCATION.ACTIVE, false).where(LOCATION.LOCATION_ID.eq(UUID.fromString(locationId))))
                .map(i -> i == 1)
                .onComplete(result);
    }

    @Override
    public final void deactivateRole(String roleId, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c -> c.update(ROLE).set(ROLE.ACTIVE, false).where(ROLE.ROLE_ID.eq(UUID.fromString(roleId))))
                .map(i -> i == 1)
                .onComplete(result);
    }

    private Optional<Ticket> rowToTicket(Row r) {
        if (r != null) {
            return Optional.of(rowToTick(r));
        }
        return Optional.empty();
    }

    private Ticket rowToTick(Row r) {
        return Ticket.builder()
                .id(r.getUUID(TICKET.TICKET_ID.getName()).toString())
                .actionId(r.getUUID(TICKET.ACTION_ID.getName()).toString())
                .locationId(r.getUUID(TICKET.LOCATION_ID.getName()).toString())
                .acquiredBy(r.getString(TICKET.AQUIRED_BY.getName()))
                .solvedBy(r.getString(TICKET.SOLVED_BY.getName()))
                .message(r.getString(TICKET.MESSAGE.getName()))
                .state(ticketToTicketState(State.valueOf(r.getString(TICKET.STATE.getName()))))
                .build();
    }


    private Future<Ticket> rowToTicketF(Row r) {
        if (r != null) {
            final Ticket ticket = rowToTick(r);
            return succeededFuture(ticket);
        }
        return failedFuture("No Row found");
    }

    private State ticketStateToState(TicketState ts) {
        return switch (ts) {
            case SOLVED -> State.SOLVED;
            case ACQUIRED -> State.ACQUIRED;
            case PENDING -> State.PENDING;
        };
    }

    private TicketState ticketToTicketState(State ts) {
        return switch (ts) {
            case SOLVED -> TicketState.SOLVED;
            case ACQUIRED -> TicketState.ACQUIRED;
            case PENDING -> TicketState.PENDING;
        };
    }

    @Override
    public final void addTicket(Ticket ticket, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c -> c.insertInto(TICKET, TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.LOCATION_ID, TICKET.MESSAGE, TICKET.STATE)
                .values(UUID.fromString(ticket.getId()), UUID.fromString(ticket.getActionId()), UUID.fromString(ticket.getLocationId()), ticket.getMessage(), ticketStateToState(ticket.getState())))
                .map(i -> i == 1)
                .onComplete(result);
    }

    @Override
    public final void updateTicket(Ticket ticket, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c ->
                c.update(TICKET)
                        .set(TICKET.AQUIRED_BY, ticket.getAcquiredBy() != null ? UUID.fromString(ticket.getAcquiredBy()) : null)
                        .set(TICKET.SOLVED_BY, ticket.getSolvedBy() != null ? UUID.fromString(ticket.getSolvedBy()) : null)
                        .set(TICKET.STATE, ticketStateToState(ticket.getState()))
                        .where(TICKET.TICKET_ID.eq(UUID.fromString(ticket.getId())))
        ).map(i -> i == 1)
                .onComplete(result);
    }


}
