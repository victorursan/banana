package com.victor.banana.services.impl;

import com.victor.banana.jooq.enums.State;
import com.victor.banana.models.events.ActionSelected;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.locations.Location;
import com.victor.banana.models.events.messages.ChatMessage;
import com.victor.banana.models.events.messages.ChatTicketMessage;
import com.victor.banana.models.events.messages.SentTicketMessage;
import com.victor.banana.models.events.personnel.PersonnelFilter;
import com.victor.banana.models.events.roles.Role;
import com.victor.banana.models.events.stickies.*;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.services.DatabaseService;
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.pgclient.PgPool;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;

import java.util.List;
import java.util.UUID;

import static com.victor.banana.controllers.db.QueryHandler.*;
import static com.victor.banana.controllers.db.RowMappers.*;
import static com.victor.banana.jooq.Tables.*;
import static com.victor.banana.utils.CallbackUtils.mergeFutures;
import static com.victor.banana.utils.Constants.DBConstants.NO_LOCATION;
import static com.victor.banana.utils.Constants.PersonnelRole.*;
import static com.victor.banana.utils.MappersHelper.mapTs;
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
        addChatQ(chat).apply(queryExecutor)
                .onComplete(result);
    }

    @Override
    public final void addPersonnel(Personnel personnel, Handler<AsyncResult<Boolean>> result) {
        addPersonnelQ(personnel).apply(queryExecutor)
                .onComplete(result);
    }

    @Override
    public final void getPersonnel(String personnelId, Handler<AsyncResult<Personnel>> result) {
        queryExecutor.findOneRow(c -> c.selectDistinct(PERSONNEL.PERSONNEL_ID, PERSONNEL.LOCATION_ID, PERSONNEL.ROLE_ID, PERSONNEL.FIRST_NAME, PERSONNEL.LAST_NAME, PERSONNEL.EMAIL)
                .from(PERSONNEL)
                .where(PERSONNEL.PERSONNEL_ID.eq(UUID.fromString(personnelId))))
                .flatMap(rowToPersonnelF())
                .onComplete(result);
    }

    @Override
    public final void findPersonnelWithUsername(PersonnelFilter filter, Handler<AsyncResult<List<Personnel>>> result) {
        final var operating = PERSONNEL.LOCATION_ID.notEqual(NO_LOCATION).and(PERSONNEL.ROLE_ID.notEqual(NO_ROLE.getUuid()));
        final var isOperating = isNotAdmin.and(filter.getOperating() ? operating : DSL.not(operating));
        filter.getUsername().ifPresentOrElse(username ->
        queryExecutor.findOneRow(c -> c.selectDistinct(PERSONNEL.PERSONNEL_ID, PERSONNEL.LOCATION_ID, PERSONNEL.ROLE_ID, PERSONNEL.FIRST_NAME, PERSONNEL.LAST_NAME, PERSONNEL.EMAIL)
                .from(PERSONNEL)
                .innerJoin(TELEGRAM_CHANNEL).using(PERSONNEL.PERSONNEL_ID)
                .where(isOperating.and(TELEGRAM_CHANNEL.USERNAME.equalIgnoreCase(username))))
                .flatMap(rowToPersonnelF())
                .map(List::of)
                .onComplete(result), () ->
                        queryExecutor.findManyRow(c -> c.selectDistinct(PERSONNEL.PERSONNEL_ID, PERSONNEL.LOCATION_ID, PERSONNEL.ROLE_ID, PERSONNEL.FIRST_NAME, PERSONNEL.LAST_NAME, PERSONNEL.EMAIL)
                                .from(PERSONNEL)
                                .where(isOperating))
                                .map(mapTs(rowToPersonnel()))
                                .onComplete(result)
                );
    }

    @Override
    public final void updatePersonnel(Personnel personnel, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c -> c.update(PERSONNEL)
                .set(PERSONNEL.FIRST_NAME, personnel.getFirstName().orElse(null))
                .set(PERSONNEL.LAST_NAME, personnel.getLastName().orElse(null))
                .set(PERSONNEL.EMAIL, personnel.getEmail().orElse(null))
                .set(PERSONNEL.LOCATION_ID, personnel.getLocationId())
                .set(PERSONNEL.ROLE_ID, personnel.getRole().getUuid())
                .where(PERSONNEL.PERSONNEL_ID.eq(personnel.getId())).and(isNotAdmin))
                .map(i -> i == 1)
                .onComplete(result);
    }

    @Override
    public final void getChat(Long chatId, Handler<AsyncResult<TelegramChannel>> result) {
        queryExecutor.findOneRow(c -> c.selectFrom(TELEGRAM_CHANNEL)
                .where(TELEGRAM_CHANNEL.CHAT_ID.eq(chatId)))
                .flatMap(rowToTelegramChannelF())
                .onComplete(result);
    }

    @Override
    public final void getChats(Ticket ticket, Handler<AsyncResult<List<Long>>> result) {
        queryExecutor.beginTransaction().flatMap(t -> {
            final var roleIdRF = t.findOneRow(c -> c.select(STICKY_ACTION.ROLE_ID)
                    .from(STICKY_ACTION)
                    .where(STICKY_ACTION.ACTION_ID.eq(ticket.getActionId())));
            final var locationIdsRF = t.findOneRow(c -> c.select(LOCATION.LOCATION_ID, LOCATION.PARENT_LOCATION).from(LOCATION)
                    .where(LOCATION.LOCATION_ID.eq(ticket.getLocationId())));
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
                }
                return failedFuture(compositeFuture.cause());
            }).flatMap(rows -> {
                if (rows != null) {
                    final var ids = rows.stream()
                            .map(r -> r.getLong(TELEGRAM_CHANNEL.CHAT_ID.getName()))
                            .collect(toList());
                    return succeededFuture(ids);
                }
                return failedFuture("No Rows found");
            })
                    .onSuccess(ignore -> t.commit())
                    .onFailure(e -> {
                        log.error("Failed to get chats", e);
                        t.rollback();
                    });
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
                .map(mapTs(rowToTicket()))
                .onComplete(result);
    }

    @Override
    public final void addMessage(ChatMessage chatMessage, Handler<AsyncResult<Boolean>> result) {
        addMessageQ(chatMessage)
                .apply(queryExecutor)
                .onComplete(result);
    }

    @Override
    public final void addTicketsMessage(List<SentTicketMessage> chatMessages, Handler<AsyncResult<Boolean>> result) {
        addTicketsMessageQ(chatMessages)
                .apply(queryExecutor)
                .onComplete(result);
    }

    @Override
    public final void getTicketMessageForTicket(String ticketId, Handler<AsyncResult<List<ChatTicketMessage>>> result) {
        queryExecutor.findManyRow(c -> c.selectDistinct(CHAT_TICKET_MESSAGE.MESSAGE_ID, CHAT_TICKET_MESSAGE.CHAT_ID, CHAT_TICKET_MESSAGE.TICKET_ID)
                .from(CHAT_TICKET_MESSAGE)
                .innerJoin(TELEGRAM_CHANNEL).using(TELEGRAM_CHANNEL.CHAT_ID)
                .innerJoin(PERSONNEL).using(TELEGRAM_CHANNEL.PERSONNEL_ID)
                .where(CHAT_TICKET_MESSAGE.TICKET_ID.eq(UUID.fromString(ticketId)).and(PERSONNEL.CHECKED_IN.eq(true))))
                .map(mapTs(rowToChatTicketMessage()))
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
                        .where(TELEGRAM_CHANNEL.CHAT_ID.eq(chatId).and(TICKET.STATE.eq(state))))
                .map(mapTs(rowToTicket()))
                .onComplete(result);
    }

    @Override
    public final void getTicketForMessage(Long chatId, Long messageId, Handler<AsyncResult<Ticket>> result) {
        queryExecutor.findOneRow(c ->
                c.selectDistinct(TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.LOCATION_ID, TICKET.AQUIRED_BY, TICKET.SOLVED_BY, TICKET.MESSAGE, TICKET.STATE)
                        .from(CHAT_TICKET_MESSAGE)
                        .innerJoin(TICKET)
                        .using(TICKET.TICKET_ID)
                        .where(CHAT_TICKET_MESSAGE.CHAT_ID.eq(chatId).and(CHAT_TICKET_MESSAGE.MESSAGE_ID.eq(messageId))))
                .flatMap(rowToTicketF())
                .onComplete(result);
    }

    @Override
    public final void getStickyLocation(String stickyLocationId, Handler<AsyncResult<StickyLocation>> result) {
        queryExecutor.beginTransaction().flatMap(t -> {
            final var stickyQ = t.findOneRow(c ->
                    c.selectDistinct(STICKY.STICKY_ID, STICKY.MESSAGE)
                            .from(STICKY)
                            .innerJoin(STICKY_LOCATION).using(STICKY.STICKY_ID)
                            .where(STICKY_LOCATION.LOCATION_ID.eq(UUID.fromString(stickyLocationId))).and(STICKY.ACTIVE.eq(true)));
            return stickyQ.flatMap(stickyR -> {
                if (stickyR != null) {
                    final var stickyId = stickyR.getUUID(STICKY.STICKY_ID.getName());
                    final var actionsQ = t.findManyRow(c ->
                            c.select(STICKY_ACTION.ACTION_ID, STICKY_ACTION.MESSAGE, STICKY_ACTION.ROLE_ID)
                                    .from(STICKY_ACTION)
                                    .where(STICKY_ACTION.STICKY_ID.eq(stickyId)
                                            .and(STICKY_ACTION.ACTIVE.eq(true))));

                    return actionsQ.flatMap(actionsR -> {
                        if (actionsR != null) {
                            final var sticky = StickyLocation.builder()
                                    .id(stickyId)
                                    .locationId(UUID.fromString(stickyLocationId))
                                    .message(stickyR.getString(STICKY.MESSAGE.getName()))
                                    .actions(mapTs(rowToAction()).apply(actionsR))
                                    .build();
                            return succeededFuture(sticky);
                        }
                        return failedFuture("No Row found");
                    });
                }
                return failedFuture("No Row found");
            })
                    .onSuccess(ignore -> t.commit())
                    .onFailure(e -> {
                        log.error("Failed to get sticky location", e);
                        t.rollback();
                    });
        }).onComplete(result);
    }

    @Override
    public final void getLocations(Handler<AsyncResult<List<Location>>> result) {
        queryExecutor.findManyRow(c -> c.select(LOCATION.LOCATION_ID, LOCATION.PARENT_LOCATION, LOCATION.MESSAGE).from(LOCATION)
                .leftOuterJoin(STICKY_LOCATION).using(LOCATION.LOCATION_ID)
                .where(STICKY_LOCATION.LOCATION_ID.isNull().and(LOCATION.ACTIVE.eq(true))))
                .map(mapTs(rowToLocation()))
                .onComplete(result);
    }

    @Override
    public void getRoles(Handler<AsyncResult<List<Role>>> result) {
        queryExecutor.findManyRow(c -> c.select(ROLE.ROLE_ID, ROLE.ROLE_TYPE).from(ROLE).where(ROLE.ACTIVE.eq(true)))
                .map(mapTs(rowToRole()))
                .onComplete(result);
    }

    @Override
    public final void getStickyAction(ActionSelected actionSelected, Handler<AsyncResult<StickyAction>> result) {
        final var PARENT_LOCATION = LOCATION.as("parent_location");
        queryExecutor.findOneRow(c -> c.select(STICKY.MESSAGE.as("sticky_message"), STICKY_ACTION.MESSAGE.as("action_message"),
                PARENT_LOCATION.MESSAGE.as("parent_location"), STICKY_LOCATION.MESSAGE.as("location"), STICKY_ACTION.ACTION_ID, STICKY_LOCATION.LOCATION_ID)
                .from(STICKY_ACTION)
                .innerJoin(STICKY).using(STICKY_ACTION.STICKY_ID)
                .innerJoin(STICKY_LOCATION).using(STICKY.STICKY_ID)
                .innerJoin(LOCATION).using(STICKY_LOCATION.LOCATION_ID)
                .innerJoin(PARENT_LOCATION).on(LOCATION.PARENT_LOCATION.eq(PARENT_LOCATION.LOCATION_ID))
                .where(STICKY_ACTION.ACTION_ID.eq(actionSelected.getActionId()))
                .and(STICKY_LOCATION.LOCATION_ID.eq(actionSelected.getLocationId()))
                .and(STICKY.ACTIVE.eq(true)))
                .flatMap(rowToStickyActionF())
                .onComplete(result);
    }

    @Override
    public final void addSticky(Sticky sticky, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.beginTransaction().flatMap(t ->
                addStickyQ(sticky).apply(t)
                        .flatMap(e -> {
                            if (e) {
                                final var addActions = addStickyActionsQ(sticky.getId(), sticky.getActions()).apply(t);
                                final var addStickyLocations = addStickyLocationsQ(sticky.getId(), sticky.getLocations()).apply(t);
                                return CompositeFuture.join(addActions, addStickyLocations)
                                        .map(c -> addActions.result() && addStickyLocations.result());
                            }
                            return failedFuture("Failed to insert sticky");
                        })
                        .onComplete(commitUpdateTransaction(t)))
                .onComplete(result);
    }

    @Override
    public final void getSticky(String stickyIdS, Handler<AsyncResult<Sticky>> result) {
        final var stickyId = UUID.fromString(stickyIdS);
        queryExecutor.beginTransaction().flatMap(t ->
                t.findOneRow(c -> c.select(STICKY.MESSAGE).from(STICKY).where(STICKY.STICKY_ID.eq(stickyId)))
                        .flatMap(s -> {
                            final var locationsF = t.findManyRow(c -> c.select(STICKY_LOCATION.LOCATION_ID, LOCATION.PARENT_LOCATION, STICKY_LOCATION.MESSAGE)
                                    .from(STICKY_LOCATION)
                                    .innerJoin(LOCATION).using(LOCATION.LOCATION_ID)
                                    .where(STICKY_LOCATION.STICKY_ID.eq(stickyId)).and(LOCATION.ACTIVE.eq(true)))
                                    .map(mapTs(rowToLocation()));
                            final var actionsF = t.findManyRow(c -> c.select(STICKY_ACTION.ACTION_ID, STICKY_ACTION.ROLE_ID, STICKY_ACTION.MESSAGE)
                                    .from(STICKY_ACTION)
                                    .where(STICKY_ACTION.STICKY_ID.eq(stickyId)).and(STICKY_ACTION.ACTIVE.eq(true)))
                                    .map(mapTs(rowToAction()));

                            return CompositeFuture.all(locationsF, actionsF)
                                    .map(c -> Sticky.builder()
                                            .id(stickyId)
                                            .message(s.getString(STICKY.MESSAGE.getName()))
                                            .actions(actionsF.result())
                                            .locations(locationsF.result())
                                            .build());
                        })
                        .onComplete(commitTransaction(t))
                        .onSuccess(ignore -> t.commit())
                        .onFailure(e -> {
                            log.error("Failed to get sticky", e);
                            t.rollback();
                        }))
                .onComplete(result);
    }

    @Override
    public final void updateStickyActions(String stickyIdS, UpdateStickyAction updates, Handler<AsyncResult<Boolean>> result) {
        final var stickyId = UUID.fromString(stickyIdS);
        queryExecutor.beginTransaction().flatMap(t -> {
            final var addActions = addStickyActionsQ(stickyId, updates.getAdd()).apply(t);
            final var updateActions = updates.getUpdate().stream().map(update -> updateStickyActionQ(update).apply(t)).collect(toList());
            final var activateActions = activateActionsQ(updates.getActivate()).apply(t);
            final var deactivateActions = deactivateActionsQ(updates.getRemove()).apply(t);
            updateActions.addAll(List.of(addActions, activateActions, deactivateActions));
            return mergeFutures(updateActions)
                    .map(c -> true)
                    .onComplete(commitUpdateTransaction(t));
        }).onComplete(result);
    }

    @Override
    public final void updateStickyLocation(String stickyIdS, UpdateStickyLocation updates, Handler<AsyncResult<Boolean>> result) {
        final var stickyId = UUID.fromString(stickyIdS);
        queryExecutor.beginTransaction().flatMap(t -> {
            final var addStickyLocations = addStickyLocationsQ(stickyId, updates.getAdd()).apply(t);
            final var updateLocations = updates.getUpdate().stream().map(update -> updateStickyLocationQ(update).apply(t)).collect(toList());
            final var activateLocations = activateStickyLocationsQ(updates.getActivate()).apply(t);
            final var deactivateLocations = deactivateStickyLocationsQ(updates.getRemove()).apply(t);
            updateLocations.addAll(List.of(activateLocations, addStickyLocations, deactivateLocations));
            return mergeFutures(updateLocations)
                    .map(c -> c.stream().anyMatch(a -> a == true))
                    .onComplete(commitUpdateTransaction(t));
        }).onComplete(result);
    }

    @Override
    public final void addLocation(Location location, Handler<AsyncResult<Boolean>> result) {
        addLocationQ(location)
                .apply(queryExecutor)
                .onComplete(result);
    }

    @Override
    public final void addRole(Role role, Handler<AsyncResult<Boolean>> result) {
        addRoleQ(role)
                .apply(queryExecutor)
                .onComplete(result);
    }

    @Override
    public final void getActiveTicketForActionSelected(ActionSelected actionSelected, Handler<AsyncResult<Ticket>> result) {
        queryExecutor.findOneRow(c ->
                c.select(TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.LOCATION_ID, TICKET.AQUIRED_BY, TICKET.SOLVED_BY, TICKET.MESSAGE, TICKET.STATE).from(TICKET)
                        .where(TICKET.ACTION_ID.eq(actionSelected.getActionId()))
                        .and(TICKET.LOCATION_ID.eq(actionSelected.getLocationId()))
                        .and(TICKET.STATE.in(List.of(State.PENDING, State.ACQUIRED))))
                .flatMap(rowToTicketF())
                .onComplete(result);
    }

    @Override
    public final void getTicket(String ticketId, Handler<AsyncResult<Ticket>> result) {
        queryExecutor.findOneRow(c -> c.select(TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.LOCATION_ID, TICKET.AQUIRED_BY, TICKET.SOLVED_BY, TICKET.MESSAGE, TICKET.STATE).from(TICKET)
                .where(TICKET.TICKET_ID.eq(UUID.fromString(ticketId))))
                .flatMap(rowToTicketF())
                .onComplete(result);
    }

    @Override
    public final void getTickets(Handler<AsyncResult<List<Ticket>>> result) {
        queryExecutor.findManyRow(c -> c.select(TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.LOCATION_ID, TICKET.AQUIRED_BY, TICKET.SOLVED_BY, TICKET.MESSAGE, TICKET.STATE).from(TICKET))
                .map(mapTs(rowToTicket()))
                .onComplete(result);
    }

    @Override
    public final void setStickyStatus(StickyStatus stickyStatus, Handler<AsyncResult<Boolean>> result) {
        setStickyStatusQ(stickyStatus.getId(), stickyStatus.getStatus()).apply(queryExecutor)
                .onComplete(result);
//        queryExecutor.beginTransaction().flatMap(t -> {
//            if (stickyStatus.getStatus()) { //todo think about it
//                return setStickyStatusQ(stickyStatus.getId(), true).apply(t);
//            } else {
//                final var stickyF = setStickyStatusQ(stickyStatus.getId(), false).apply(t);
//                final var stickyActionsF = deactivateStickyActionsQ(stickyStatus.getId()).apply(t);
//                final var deactivateLocationsF = deactivateStickyLocationsQ(stickyStatus.getId()).apply(t);
//                return CompositeFuture.all(stickyActionsF, stickyF, deactivateLocationsF)
//                        .map(i -> stickyActionsF.result() && stickyF.result() && deactivateLocationsF.result())
//                        .onComplete(commitUpdateTransaction(t));
//            }
//        }).onComplete(result);
    }

    @Override
    public final void updateStickyMessage(StickyMessage stickyMessage, Handler<AsyncResult<Boolean>> result) {
        updateStickyMessageQ(stickyMessage.getId(), stickyMessage.getMessage()).apply(queryExecutor)
                .onComplete(result);
    }

    @Override
    public final void deactivateLocation(String locationId, Handler<AsyncResult<Boolean>> result) {
        deactivateLocationQ(UUID.fromString(locationId))
                .apply(queryExecutor)
                .onComplete(result);
    }

    @Override
    public final void deactivateRole(String roleId, Handler<AsyncResult<Boolean>> result) {
        deactivateRoleQ(UUID.fromString(roleId))
                .apply(queryExecutor)
                .onComplete(result);
    }

    @Override
    public final void addTicket(Ticket ticket, Handler<AsyncResult<Boolean>> result) {
        addTicketQ(ticket)
                .apply(queryExecutor)
                .onComplete(result);
    }

    @Override
    public final void updateTicket(Ticket ticket, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c ->
                c.update(TICKET)
                        .set(TICKET.AQUIRED_BY, ticket.getAcquiredBy().orElse(null))
                        .set(TICKET.SOLVED_BY, ticket.getSolvedBy().orElse(null))
                        .set(TICKET.STATE, ticketStateToState(ticket.getState()))
                        .where(TICKET.TICKET_ID.eq(ticket.getId())))
                .map(i -> i == 1)
                .onComplete(result);
    }
}
