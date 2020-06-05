package com.victor.banana.controllers.db;

import com.victor.banana.jooq.enums.State;
import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.locations.Location;
import com.victor.banana.models.events.messages.ChatMessage;
import com.victor.banana.models.events.messages.SentDeleteMessage;
import com.victor.banana.models.events.messages.SentTicketMessage;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.roles.Role;
import com.victor.banana.models.events.stickies.Action;
import com.victor.banana.models.events.stickies.Sticky;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketNotification;
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record9;
import org.jooq.SelectJoinStep;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.victor.banana.controllers.db.RowMappers.*;
import static com.victor.banana.jooq.Tables.*;
import static com.victor.banana.utils.Constants.PersonnelRole.ADMIN;
import static com.victor.banana.utils.MappersHelper.mapTs;
import static io.vertx.core.Future.failedFuture;
import static org.jooq.impl.DSL.arrayAgg;


public final class QueryHandler {
    public static final Condition isNotAdmin = PERSONNEL.ROLE_ID.notEqual(ADMIN.getUuid());
    private static final Logger log = LoggerFactory.getLogger(QueryHandler.class);

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> addStickyActionsQ(UUID stickyId, List<Action> actions) {
        return t -> {
            if (actions.isEmpty()) {
                return Future.succeededFuture(true);
            }
            return t.execute(c -> {
                final var insert = c.insertInto(STICKY_ACTION, STICKY_ACTION.ACTION_ID, STICKY_ACTION.STICKY_ID, STICKY_ACTION.ROLE_ID, STICKY_ACTION.MESSAGE);
                actions.forEach(action -> insert.values(action.getId(), stickyId, action.getRoleId(), action.getMessage()));
                return insert;
            })
                    .map(i -> i == actions.size());
        };
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> activateActionsQ(List<UUID> toActivate) {
        return t -> {
            if (toActivate.isEmpty()) {
                return Future.succeededFuture(true);
            }
            return t.execute(c -> c.update(STICKY_ACTION).set(STICKY_ACTION.ACTIVE, true).where(STICKY_ACTION.ACTION_ID.in(toActivate)))
                    .map(i -> i == toActivate.size());
        };
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> updateStickyActionQ(Action actionUpdate) {
        return t -> t.execute(c ->
                c.update(STICKY_ACTION)
                        .set(STICKY_ACTION.ROLE_ID, actionUpdate.getRoleId())
                        .set(STICKY_ACTION.MESSAGE, actionUpdate.getMessage())
                        .where(STICKY_ACTION.ACTION_ID.eq(actionUpdate.getId())))
                .map(i -> i == 1);
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> updateStickyLocationQ(Location locationUpdate) {
        return t -> t.execute(c -> c.update(STICKY_LOCATION) //todo rethink
                .set(STICKY_LOCATION.MESSAGE, locationUpdate.getText())
                .where(STICKY_LOCATION.LOCATION_ID.eq(locationUpdate.getId())))
                .flatMap(i -> {
                    if (i == 1) {
                        return t.execute(cc -> cc.update(LOCATION)
                                .set(LOCATION.MESSAGE, locationUpdate.getText())
                                .set(LOCATION.PARENT_LOCATION, locationUpdate.getParentLocation())
                                .where(LOCATION.LOCATION_ID.eq(locationUpdate.getId())))
                                .map(ii -> ii == 1);
                    }
                    return Future.succeededFuture(false);
                });
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> deactivateActionsQ(List<UUID> toDeactivate) {
        return t -> {
            if (toDeactivate.isEmpty()) {
                return Future.succeededFuture(true);
            }
            return activateActionsWhere(STICKY_ACTION.ACTION_ID.in(toDeactivate)).apply(t)
                    .map(i -> i == toDeactivate.size());
        };
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

    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<Action>>> getActionsForSticky(UUID stickyId) {
        return t -> t.findManyRow(c -> c.select(STICKY_ACTION.ACTION_ID, STICKY_ACTION.ROLE_ID, STICKY_ACTION.MESSAGE)
                .from(STICKY_ACTION)
                .where(STICKY_ACTION.STICKY_ID.eq(stickyId)).and(STICKY_ACTION.ACTIVE.eq(true)))
                .map(mapTs(rowToAction()));
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<Action>>> getActionsForStickyLocation(UUID stickyId, UUID locationId) {
        return t -> t.findManyRow(c -> c.select(STICKY_ACTION.ACTION_ID, STICKY_ACTION.ROLE_ID, STICKY_ACTION.MESSAGE, TICKET.TICKET_ID)
                .from(STICKY_ACTION)
                .leftJoin(TICKET).on(TICKET.LOCATION_ID.eq(locationId).and(TICKET.ACTION_ID.eq(STICKY_ACTION.ACTION_ID)).and(TICKET.STATE.in(List.of(State.PENDING, State.ACQUIRED))))
                .where(STICKY_ACTION.STICKY_ID.eq(stickyId)).and(STICKY_ACTION.ACTIVE.eq(true)))
                .map(mapTs(rowToAction()));
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<Location>>> getLocationsForSticky(UUID stickyId) {
        return t -> t.findManyRow(c -> c.select(STICKY_LOCATION.LOCATION_ID, LOCATION.PARENT_LOCATION, STICKY_LOCATION.MESSAGE)
                .from(STICKY_LOCATION)
                .innerJoin(LOCATION).using(LOCATION.LOCATION_ID)
                .where(STICKY_LOCATION.STICKY_ID.eq(stickyId)).and(LOCATION.ACTIVE.eq(true)))
                .map(mapTs(rowToLocation()));
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<UUID>>> findAllParentLocationQ(UUID locationId) {
        final var LOCATIONS = LOCATION.as("locations");
        final var PARENT = LOCATION.as("p");
        return t -> t.findOneRow(c ->
                c.withRecursive(LOCATIONS.getUnqualifiedName()).as(
                        c.select(LOCATION.LOCATION_ID, LOCATION.PARENT_LOCATION)
                                .from(LOCATION)
                                .where(LOCATION.LOCATION_ID.eq(locationId))
                                .unionAll(c.select(PARENT.LOCATION_ID, PARENT.PARENT_LOCATION)
                                        .from(PARENT)
                                        .join(LOCATIONS).on(PARENT.LOCATION_ID.eq(LOCATIONS.PARENT_LOCATION).and(PARENT.LOCATION_ID.notEqual(PARENT.PARENT_LOCATION)))
                                )
                ).select(arrayAgg(LOCATIONS.PARENT_LOCATION).as("all_loc")).from(LOCATIONS)
        ).map(r -> Arrays.asList(r.getUUIDArray("all_loc")));
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Sticky>> getStickyFor(UUID stickyId, String stickyMessage) {
        return t -> {
            final var locationsF = getLocationsForSticky(stickyId).apply(t);
            final var actionsF = getActionsForSticky(stickyId).apply(t);
            return CompositeFuture.all(locationsF, actionsF)
                    .map(c -> Sticky.builder()
                            .id(stickyId)
                            .message(stickyMessage)
                            .actions(actionsF.result())
                            .locations(locationsF.result())
                            .build());
        };

    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> deactivateStickyLocationsQ(List<UUID> toDeactivate) {
        return t -> {
            if (toDeactivate.isEmpty()) {
                return Future.succeededFuture(true);
            }
            return deactivateStickyLocationsWhere(STICKY_LOCATION.LOCATION_ID.eq(LOCATION.LOCATION_ID)
                    .and(LOCATION.LOCATION_ID.in(toDeactivate))).apply(t)
                    .map(i -> i == toDeactivate.size());
        };
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
        return t -> {
            if (locationIds.isEmpty()) {
                return Future.succeededFuture(true);
            }
            return t.execute(c -> c.update(LOCATION).set(LOCATION.ACTIVE, false).where(LOCATION.LOCATION_ID.in(locationIds)))
                    .map(i -> i == locationIds.size());
        };
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> deactivateRoleQ(UUID roleId) {
        return t -> t.execute(c -> c.update(ROLE).set(ROLE.ACTIVE, false).where(ROLE.ROLE_ID.eq(roleId)))
                .map(i -> i == 1);
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> activateStickyLocationsQ(List<UUID> toActivate) {
        return t -> {
            if (toActivate.isEmpty()) {
                return Future.succeededFuture(true);
            }
            return t.execute(c -> c.update(LOCATION).set(LOCATION.ACTIVE, true)
                    .from(STICKY_LOCATION).where(STICKY_LOCATION.LOCATION_ID.eq(LOCATION.LOCATION_ID))
                    .and(LOCATION.LOCATION_ID.in(toActivate)))
                    .map(i -> i == toActivate.size());
        };
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> setStickyStatusQ(UUID stickyId, Boolean status) {
        return t -> t.execute(c -> c.update(STICKY).set(STICKY.ACTIVE, status).where(STICKY.STICKY_ID.eq(stickyId)))
                .map(i -> i == 1);
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> updateStickyMessageQ(UUID stickyId, String message) {
        return t -> t.execute(c -> c.update(STICKY).set(STICKY.MESSAGE, message).where(STICKY.STICKY_ID.eq(stickyId)))
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
        return commitUpdateTransaction(t, r -> {
        });
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
        return t -> {
            if (locations.isEmpty()) {
                return Future.succeededFuture(true);
            }
            return addLocationsQ(locations)
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
                    ).apply(t);
        };
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> addChatQ(TelegramChannel chat) {
        return t -> t.execute(c -> c.insertInto(TELEGRAM_CHANNEL, TELEGRAM_CHANNEL.CHAT_ID, TELEGRAM_CHANNEL.PERSONNEL_ID, TELEGRAM_CHANNEL.USERNAME)
                .values(chat.getChatId(), chat.getPersonnelId(), chat.getUsername())
                .onConflictDoNothing()
        ).map(i -> i == 1 || i == 0);
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> addPersonnelQ(Personnel personnel) {
        return t -> t.execute(c -> c.insertInto(PERSONNEL, PERSONNEL.PERSONNEL_ID, PERSONNEL.FIRST_NAME, PERSONNEL.LAST_NAME, PERSONNEL.EMAIL, PERSONNEL.LOCATION_ID, PERSONNEL.ROLE_ID)
                .values(personnel.getId(), personnel.getFirstName().orElse(null), personnel.getLastName().orElse(null), personnel.getEmail().orElse(null), personnel.getLocationId(), personnel.getRole().getUuid())
                .onConflictDoNothing()
        ).map(i -> i == 1 || i == 0);
    }


    public static SelectJoinStep<Record9<UUID, UUID, UUID, UUID, String, State, OffsetDateTime, OffsetDateTime, OffsetDateTime>> selectTicket(DSLContext c) {
        return c.selectDistinct(TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.LOCATION_ID, TICKET.OWNED_BY, TICKET.MESSAGE, TICKET.STATE, TICKET.CREATED_AT, TICKET.ACQUIRED_AT, TICKET.SOLVED_AT).from(TICKET);
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> addTicketQ(Ticket ticket) {
        return t -> t.execute(c -> c.insertInto(TICKET, TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.LOCATION_ID, TICKET.MESSAGE, TICKET.STATE)
                .values(ticket.getId(), ticket.getActionId(), ticket.getLocationId(), ticket.getMessage(), ticketStateToState(ticket.getState())))
                .map(i -> i == 1);
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> addTicketNotificationQ(TicketNotification ticketNotification) {
        return t -> t.execute(c -> c.insertInto(PERSONNEL_TICKET, PERSONNEL_TICKET.TICKET_ID, PERSONNEL_TICKET.PERSONNEL_ID, PERSONNEL_TICKET.NOTIFICATION)
                .values(ticketNotification.getTicketId(), ticketNotification.getPersonnelId(), notificationTypeToNotification(ticketNotification.getType())))
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
        return t -> {
            if (chatMessages.isEmpty()) {
                return Future.succeededFuture();
            }
            return t.execute(c -> {
                final var insert = c.insertInto(CHAT_TICKET_MESSAGE, CHAT_TICKET_MESSAGE.MESSAGE_ID, CHAT_TICKET_MESSAGE.CHAT_ID, CHAT_TICKET_MESSAGE.TICKET_ID, CHAT_TICKET_MESSAGE.VISIBLE);
                chatMessages.forEach(chatMessage ->
                        insert.values(chatMessage.getMessageId(), chatMessage.getChatId(), chatMessage.getTicketId(), true));
                return insert.onConflictDoNothing();
            })
                    .map(i -> i == chatMessages.size());
        };
    }

    public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> hideTicketsMessageQ(List<SentDeleteMessage> chatMessages) {
        return t -> t.execute(c -> {
            final var primaryKey = c.newRecord(CHAT_TICKET_MESSAGE.MESSAGE_ID, CHAT_TICKET_MESSAGE.CHAT_ID);
            final var keys = mapTs((SentDeleteMessage m) -> primaryKey.values(m.getMessageId(), m.getChatId()).valuesRow()).apply(chatMessages);
            return c.update(CHAT_TICKET_MESSAGE).set(CHAT_TICKET_MESSAGE.VISIBLE, false).where(primaryKey.fieldsRow().in(keys));
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
        return t -> {
            if (locations.isEmpty()) {
                return Future.succeededFuture(true);
            }
            return t.execute(c -> {
                final var insert = c.insertInto(LOCATION, LOCATION.LOCATION_ID, LOCATION.PARENT_LOCATION, LOCATION.MESSAGE);
                locations.forEach(location -> insert.values(location.getId(), location.getParentLocation(), location.getText()));
                return insert;
            })
                    .map(i -> i == locations.size());
        };
    }

}
