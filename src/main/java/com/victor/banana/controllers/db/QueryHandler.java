package com.victor.banana.controllers.db;

import com.victor.banana.models.events.Personnel;
import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.locations.Location;
import com.victor.banana.models.events.messages.ChatMessage;
import com.victor.banana.models.events.messages.SentTicketMessage;
import com.victor.banana.models.events.roles.Role;
import com.victor.banana.models.events.stickies.Action;
import com.victor.banana.models.events.stickies.Sticky;
import com.victor.banana.models.events.tickets.Ticket;
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.jooq.Condition;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.victor.banana.controllers.db.RowMappers.ticketStateToState;
import static com.victor.banana.jooq.Tables.*;
import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;


public final class QueryHandler {
    private static final Logger log = LoggerFactory.getLogger(QueryHandler.class);

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> addStickyActionsQ(UUID stickyId, List<Action> actions) {
        return t -> t.execute(c -> {
            final var insert = c.insertInto(STICKY_ACTION, STICKY_ACTION.ACTION_ID, STICKY_ACTION.STICKY_ID, STICKY_ACTION.ROLE_ID, STICKY_ACTION.MESSAGE);
            actions.forEach(action -> insert.values(action.getId(), stickyId, action.getRoleId(), action.getMessage()));
            return insert;
        })
                .map(i -> i == actions.size())
                .recover(e -> actions.size() == 0 ? succeededFuture(true) : failedFuture(e));
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> activateActionsQ(List<UUID> toActivate) {
        return t -> t.execute(c -> c.update(STICKY_ACTION).set(STICKY_ACTION.ACTIVE, true).where(STICKY_ACTION.ACTION_ID.in(toActivate)))
                .map(i -> i == toActivate.size())
                .recover(e -> toActivate.size() == 0 ? succeededFuture(true) : failedFuture(e));
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> deactivateActionsQ(List<UUID> toDeactivate) {
        return t -> activateActionsWhere(STICKY_ACTION.ACTION_ID.in(toDeactivate)).apply(t)
                .map(i -> i == toDeactivate.size())
                .recover(e -> toDeactivate.size() == 0 ? succeededFuture(true) : failedFuture(e));
    }

    private static Function<ReactiveClassicGenericQueryExecutor, Future<Integer>> activateActionsWhere(Condition condition) {
        return t -> t.execute(c -> c.update(STICKY_ACTION).set(STICKY_ACTION.ACTIVE, true).where(condition));
    }

    private static Function<ReactiveClassicGenericQueryExecutor, Future<Integer>> deactivateActionsWhere(Condition condition) {
        return t -> t.execute(c -> c.update(STICKY_ACTION).set(STICKY_ACTION.ACTIVE, false).where(condition));
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> activateStickyActionsQ(UUID stickyId) {
        return t -> activateActionsWhere(STICKY_ACTION.STICKY_ID.eq(stickyId)).apply(t)
                .map(i -> i > 0);
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> deactivateStickyActionsQ(UUID stickyId) {
        return t -> deactivateActionsWhere(STICKY_ACTION.STICKY_ID.eq(stickyId)).apply(t)
                .map(i -> i > 0);
    }

    private static Function<ReactiveClassicGenericQueryExecutor, Future<Integer>> deactivateStickyLocationsWhere(Condition condition) {
        return t -> t.execute(c -> c.update(LOCATION).set(LOCATION.ACTIVE, false)
                .from(STICKY_LOCATION).where(condition));
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> deactivateStickyLocationsQ(List<UUID> toDeactivate) {
        return t -> deactivateStickyLocationsWhere(STICKY_LOCATION.LOCATION_ID.eq(LOCATION.LOCATION_ID)
                .and(LOCATION.LOCATION_ID.in(toDeactivate))).apply(t)
                .map(i -> i == toDeactivate.size())
                .recover(e -> toDeactivate.size() == 0 ? succeededFuture(true) : failedFuture(e));
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> deactivateStickyLocationsQ(UUID stickyId) {
        return t -> deactivateStickyLocationsWhere(STICKY_LOCATION.LOCATION_ID.eq(LOCATION.LOCATION_ID)
                .and(STICKY_LOCATION.STICKY_ID.eq(stickyId))).apply(t)
                .map(i -> i > 0);
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> deactivateLocationQ(UUID locationId) {
        return deactivateLocationsQ(List.of(locationId));
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> deactivateLocationsQ(List<UUID> locationIds) {
        return t -> t.execute(c -> c.update(LOCATION).set(LOCATION.ACTIVE, false).where(LOCATION.LOCATION_ID.in(locationIds)))
                .map(i -> i == locationIds.size())
                .recover(e -> locationIds.size() == 0 ? succeededFuture(true) : failedFuture(e));
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> deactivateRoleQ(UUID roleId) {
        return t -> t.execute(c -> c.update(ROLE).set(ROLE.ACTIVE, false).where(ROLE.ROLE_ID.eq(roleId)))
                .map(i -> i == 1);
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> activateStickyLocationsQ(List<UUID> toActivate) {
        return t -> t.execute(c -> c.update(LOCATION).set(LOCATION.ACTIVE, true)
                .from(STICKY_LOCATION).where(STICKY_LOCATION.LOCATION_ID.eq(LOCATION.LOCATION_ID))
                .and(LOCATION.LOCATION_ID.in(toActivate)))
                .map(i -> i == toActivate.size())
                .recover(e -> toActivate.size() == 0 ? succeededFuture(true) : failedFuture(e));
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> deactivateStickyQ(UUID stickyId) {
        return t -> t.execute(c -> c.update(STICKY).set(STICKY.ACTIVE, false).where(STICKY.STICKY_ID.eq(stickyId)))
                .map(i -> i == 1);
    }

    public static <T extends ReactiveClassicGenericQueryExecutor> Handler<AsyncResult<Boolean>> commitUpdateTransaction(T t) {
        return commitUpdateTransaction(t, r -> {
            if (r) {
                t.commit();
            } else {
                t.rollback();
            }
        });
    }

    public static <T extends ReactiveClassicGenericQueryExecutor, R> Handler<AsyncResult<R>> commitTransaction(T t) {
        return commitUpdateTransaction(t, r -> {});
    }

    private static <T extends ReactiveClassicGenericQueryExecutor, R> Handler<AsyncResult<R>> commitUpdateTransaction(T t, Consumer<R> onSuccess) {
        return r -> {
            if (r.succeeded()) {
                onSuccess.accept(r.result());
                t.commit();
            } else {
                log.error("failed to commit transaction", r.cause());
                t.rollback();
            }
        };
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> addStickyLocationsQ(UUID stickyId, List<Location> locations) {
        return t -> addLocationsQ(locations)
                .andThen(locF ->
                        locF.flatMap(loc -> {
                            if (loc) {
                                return t.execute(c -> {
                                    final var insert = c.insertInto(STICKY_LOCATION, STICKY_LOCATION.LOCATION_ID, STICKY_LOCATION.STICKY_ID, STICKY_LOCATION.MESSAGE);
                                    locations.forEach(location -> insert.values(location.getId(), stickyId, location.getText()));
                                    return insert;
                                });
                            }
                            return failedFuture("Failed to insert location");
                        })
                                .map(i -> i == locations.size())
                                .recover(e -> locations.size() == 0 ? succeededFuture(true) : failedFuture(e))
                ).apply(t);
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> addChatQ(TelegramChannel chat) {
        return t -> t.execute(c -> c.insertInto(TELEGRAM_CHANNEL, TELEGRAM_CHANNEL.CHAT_ID, TELEGRAM_CHANNEL.PERSONNEL_ID, TELEGRAM_CHANNEL.USERNAME)
                .values(chat.getChatId(), chat.getPersonnelId(), chat.getUsername())
                .onConflictDoNothing()
        ).map(i -> i == 1 || i == 0);
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> addPersonnelQ(Personnel personnel) {
        return t -> t.execute(c -> c.insertInto(PERSONNEL, PERSONNEL.PERSONNEL_ID, PERSONNEL.FIRST_NAME, PERSONNEL.LAST_NAME, PERSONNEL.LOCATION_ID, PERSONNEL.ROLE_ID)
                .values(personnel.getId(), personnel.getFirstName(), personnel.getLastName(), personnel.getLocationId(), personnel.getRoleId())
                .onConflictDoNothing()
        ).map(i -> i == 1 || i == 0);
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> addTicketQ(Ticket ticket) {
        return t -> t.execute(c -> c.insertInto(TICKET, TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.LOCATION_ID, TICKET.MESSAGE, TICKET.STATE)
                .values(ticket.getId(), ticket.getActionId(), ticket.getLocationId(), ticket.getMessage(), ticketStateToState(ticket.getState())))
                .map(i -> i == 1);
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> addStickyQ(Sticky sticky) {
        return t -> t.execute(c -> c.insertInto(STICKY, STICKY.STICKY_ID, STICKY.MESSAGE)
                .values(sticky.getId(), sticky.getMessage()))
                .map(i -> i == 1);
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> addMessageQ(ChatMessage chatMessage) {
        return t -> t.execute(c -> c.insertInto(CHAT_MESSAGE, CHAT_MESSAGE.MESSAGE_ID, CHAT_MESSAGE.CHAT_ID, CHAT_MESSAGE.MESSAGE)
                .values(chatMessage.getMessageId(), chatMessage.getChatId(), chatMessage.getMessage()))
                .map(i -> i == 1);
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> addTicketsMessageQ(List<SentTicketMessage> chatMessages) {
        return t -> t.execute(c -> {
            final var insert = c.insertInto(CHAT_TICKET_MESSAGE, CHAT_TICKET_MESSAGE.MESSAGE_ID, CHAT_TICKET_MESSAGE.CHAT_ID, CHAT_TICKET_MESSAGE.TICKET_ID);
            chatMessages.forEach(chatMessage ->
                    insert.values(chatMessage.getMessageId(), chatMessage.getChatId(), chatMessage.getTicketId()));
            return insert.onConflictDoNothing();
        })
                .map(i -> i == chatMessages.size());
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> addLocationQ(Location location) {
        return addLocationsQ(List.of(location));
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> addRoleQ(Role role) {
        return t -> t.execute(c ->
                c.insertInto(ROLE, ROLE.ROLE_ID, ROLE.ROLE_TYPE)
                        .values(role.getId(), role.getType()))
                .map(i -> i == 1);
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> addLocationsQ(List<Location> locations) {
        return t -> t.execute(c -> {
            final var insert = c.insertInto(LOCATION, LOCATION.LOCATION_ID, LOCATION.PARENT_LOCATION, LOCATION.MESSAGE);
            locations.forEach(location -> insert.values(location.getId(), location.getParentLocation(), location.getText()));
            return insert;
        })
                .map(i -> i == locations.size())
                .recover(e -> locations.size() == 0 ? succeededFuture(true) : failedFuture(e));
    }

}
