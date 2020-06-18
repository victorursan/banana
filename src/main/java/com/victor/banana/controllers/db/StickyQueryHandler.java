package com.victor.banana.controllers.db;

import com.victor.banana.jooq.enums.State;
import com.victor.banana.models.events.ActionSelected;
import com.victor.banana.models.events.locations.StickyLocation;
import com.victor.banana.models.events.stickies.*;
import com.victor.banana.utils.CallbackUtils;
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor;
import io.vertx.core.Future;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jooq.Record;
import org.jooq.*;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.victor.banana.controllers.db.LocationQueryHandler.getStickyLocationsForStickyIdQ;
import static com.victor.banana.controllers.db.QueryHandler.*;
import static com.victor.banana.controllers.db.RowMappers.*;
import static com.victor.banana.jooq.Tables.*;
import static com.victor.banana.utils.CallbackUtils.mergeFutures;
import static com.victor.banana.utils.MappersHelper.flatMapTsF;
import static io.vertx.core.Future.succeededFuture;
import static java.util.stream.Collectors.toList;
import static org.jooq.impl.DSL.*;

public final class StickyQueryHandler {

    private StickyQueryHandler() {
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> updateStickyLocationsQ(UpdateStickyLocation updates) {
        return withTransaction(t -> {
            final var addStickyLocations = addStickyLocationsQ(updates.getAdd()).apply(t);
            final var updateLocations = flatMapTsF((StickyLocation update) -> updateStickyLocationQ(update).apply(t)).apply(updates.getUpdate());
            final var activateLocations = activateStickyLocationsQ(updates.getActivate()).apply(t);
            final var deactivateLocations = deactivateStickyLocationsQ(updates.getRemove()).apply(t);
            return mergeFutures(activateLocations, addStickyLocations, deactivateLocations, updateLocations)
                    .mapEmpty();
        });
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> updateStickyActionsQ(UpdateStickyAction updates) {
        return withTransaction(t -> {
            final var addActions = addStickyActionsQ(updates.getAdd()).apply(t);
            final var updateActions = flatMapTsF((Action update) -> updateStickyActionQ(update).apply(t)).apply(updates.getUpdate());
            final var activateActions = activateActionsQ(updates.getActivate()).apply(t);
            final var deactivateActions = deactivateActionsQ(updates.getRemove()).apply(t);
            return mergeFutures(addActions, activateActions, deactivateActions, updateActions)
                    .mapEmpty();
        });
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<StickyAction>> getStickyActionQ(ActionSelected actionSelected) {
        return findOne(c -> c.select(STICKY_ACTION.NAME, FLOOR.NAME.as("floor"), STICKY_LOCATION.NAME.as("location"), STICKY_ACTION.ACTION_ID, STICKY_LOCATION.LOCATION_ID)
                        .from(STICKY_ACTION)
                        .innerJoin(STICKY).using(STICKY_ACTION.STICKY_ID)
                        .innerJoin(STICKY_LOCATION).using(STICKY.STICKY_ID)
                        .innerJoin(FLOOR).using(STICKY_LOCATION.FLOOR_ID)
                        .where(STICKY_ACTION.ACTION_ID.eq(actionSelected.getActionId()))
                        .and(STICKY_LOCATION.LOCATION_ID.eq(actionSelected.getLocationId()))
                        .and(STICKY.ACTIVE.eq(true)),
                rowToStickyAction());
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addStickyQ(Sticky sticky) {
        return withTransaction(t ->
                addStickyTQ(sticky).apply(t)
                        .flatMap(ignore -> {
                            final var addActions = addStickyActionsQ(sticky.getActions()).apply(t);
                            final var addStickyLocations = addStickyLocationsQ(sticky.getStickyLocations()).apply(t);
                            return CallbackUtils.mergeFutures(addActions, addStickyLocations)
                                    .mapEmpty();
                        }));
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Sticky>> getStickyQ(UUID stickyId) {
        return withTransaction(t ->
                getStickyTitle(stickyId).apply(t)
                        .flatMap(stickyTitle -> getStickyFor(stickyTitle).apply(t)));
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<Sticky>>> getStickiesQ() {
        return withTransaction(t ->
                findMany(selectStickyTitle(), rowToStickyTitle()).apply(t)
                        .flatMap(stickyTitles -> {
                            final var listOfF = stickyTitles.stream()
                                    .map(stickyTitle -> getStickyFor(stickyTitle).apply(t))
                                    .collect(toList());
                            return CallbackUtils.mergeFutures(listOfF);
                        }));
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<ScanSticky>> getScanStickyQ(UUID locationId) {
        return withTransaction(t -> {
            final var stickyQ = findOne(selectStickyTitle().andThen(sticky -> sticky.innerJoin(STICKY_LOCATION).using(STICKY.STICKY_ID)
                    .where(STICKY_LOCATION.LOCATION_ID.eq(locationId)).and(STICKY.ACTIVE.eq(true))), rowToStickyTitle())
                    .apply(t);
            return stickyQ.flatMap(stickyTitle -> {
                final var actionsQ = getActionsForStickyLocation(stickyTitle.getId(), locationId).apply(t);
                return actionsQ.map(actionsR -> ScanSticky.builder()
                        .id(stickyTitle.getId())
                        .locationId(locationId)
                        .message(stickyTitle.getTitle())
                        .actions(actionsR)
                        .build());
            });
        });
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> updateStickyTitleQ(StickyTitle stickyTitle) {
        return execute(c -> c.update(STICKY)
                        .set(STICKY.TITLE, stickyTitle.getTitle())
                        .set(STICKY.ACTIVE, stickyTitle.getActive())
                        .where(STICKY.STICKY_ID.eq(stickyTitle.getId())),
                1, "update sticky message");
    }

    @NotNull
    private static Function<ReactiveClassicGenericQueryExecutor, Future<StickyTitle>> getStickyTitle(UUID stickyId) {
        return findOne(selectStickyTitle(STICKY.STICKY_ID.eq(stickyId)), rowToStickyTitle());
    }

    @NotNull
    private static Function<DSLContext, SelectConditionStep<Record3<UUID, String, Boolean>>> selectStickyTitle(Condition condition) {
        return selectStickyTitle().andThen(a -> a.where(condition));
    }

    @NotNull
    private static Function<DSLContext, SelectJoinStep<Record3<UUID, String, Boolean>>> selectStickyTitle() {
        return c -> c.select(STICKY.STICKY_ID, STICKY.TITLE, STICKY.ACTIVE).from(STICKY);
    }

    @NotNull
    private static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addStickyTQ(Sticky sticky) {
        return execute(c -> c.insertInto(STICKY, STICKY.STICKY_ID, STICKY.TITLE)
                        .values(sticky.getId(), sticky.getTitle()),
                1, "add sticky");
    }

    @NotNull
    private static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addStickyLocationsQ(List<StickyLocation> stickyLocations) {
        if (stickyLocations.isEmpty()) {
            return t -> succeededFuture();
        }
        return execute(c -> {
            final var insert = c.insertInto(STICKY_LOCATION, STICKY_LOCATION.LOCATION_ID, STICKY_LOCATION.STICKY_ID, STICKY_LOCATION.FLOOR_ID, STICKY_LOCATION.NAME);
            stickyLocations.forEach(location -> insert.values(location.getId(), location.getStickyId(), location.getFloorId(), location.getName()));
            return insert;
        }, stickyLocations.size(), "insert sticky location");
    }


    @NotNull
    private static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> activateStickyLocationsQ(List<UUID> toActivate) {
        if (toActivate.isEmpty()) {
            return t -> succeededFuture();
        }
        return execute(activateStickyLocationsWhere(STICKY_LOCATION.LOCATION_ID.in(toActivate)),
                toActivate.size(), "activate sticky location");
    }

    @NotNull
    private static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> deactivateStickyLocationsQ(List<UUID> toDeactivate) {
        if (toDeactivate.isEmpty()) {
            return t -> succeededFuture();
        }
        return execute(deactivateStickyLocationsWhere(STICKY_LOCATION.LOCATION_ID.in(toDeactivate)),
                toDeactivate.size(), "deactivate sticky location");
    }

    @NotNull
    private static Function<ReactiveClassicGenericQueryExecutor, Future<Sticky>> getStickyFor(StickyTitle stickyTitle) {
        return t -> {
            final var locationsF = getStickyLocationsForStickyIdQ(stickyTitle.getId()).apply(t);
            final var actionsF = getActionsForSticky(stickyTitle.getId()).apply(t);
            return CallbackUtils.mergeFutures(locationsF, actionsF)
                    .map(i -> Sticky.builder()
                            .id(stickyTitle.getId())
                            .title(stickyTitle.getTitle())
                            .active(stickyTitle.getActive())
                            .actions(actionsF.result())
                            .stickyLocations(locationsF.result())
                            .build());
        };

    }

    @NotNull
    private static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> activateActionsQ(List<UUID> toActivate) {
        if (toActivate.isEmpty()) {
            return t -> succeededFuture();
        }
        return execute(activateActionsWhere(STICKY_ACTION.ACTION_ID.in(toActivate)),
                toActivate.size(), "activate actions");
    }

    @NotNull
    private static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> deactivateActionsQ(List<UUID> toDeactivate) {
        if (toDeactivate.isEmpty()) {
            return t -> succeededFuture();
        }
        return execute(deactivateActionsWhere(STICKY_ACTION.ACTION_ID.in(toDeactivate)),
                toDeactivate.size(), "deactivate actions");
    }

    @NotNull
    private static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> updateStickyActionQ(Action actionUpdate) {
        return execute(c ->
                        c.update(STICKY_ACTION)
                                //.set(STICKY_ACTION.ROLE_ID, actionUpdate.getRoles()) todo
                                .set(STICKY_ACTION.NAME, actionUpdate.getName())
                                .set(STICKY_ACTION.DESCRIPTION, actionUpdate.getDescription())
                                .where(STICKY_ACTION.ACTION_ID.eq(actionUpdate.getId())),
                1, "update action");
    }

    @NotNull
    private static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> updateStickyLocationQ(StickyLocation stickyLocationUpdate) {
        return execute(c -> c.update(STICKY_LOCATION) //todo rethink
                        .set(STICKY_LOCATION.NAME, stickyLocationUpdate.getName())
                        .set(STICKY_LOCATION.FLOOR_ID, stickyLocationUpdate.getFloorId())
                        .where(STICKY_LOCATION.LOCATION_ID.eq(stickyLocationUpdate.getId())),
                1, "update sticky location");
    }

    @NotNull
    private static Function<DSLContext, UpdateConditionStep<Record>> activateActionsWhere(Condition condition) {
        return c -> c.update(STICKY_ACTION).set(STICKY_ACTION.ACTIVE, true).where(condition);
    }

    @NotNull
    private static Function<DSLContext, UpdateConditionStep<Record>> deactivateActionsWhere(Condition condition) {
        return c -> c.update(STICKY_ACTION).set(STICKY_ACTION.ACTIVE, false).where(condition);
    }

//    @NotNull
// public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> activateStickyActionsQ(UUID stickyId) {
//        return t -> activateActionsWhere(STICKY_ACTION.STICKY_ID.eq(stickyId)).apply(t)
//                .map(i -> i > 0);
//    }
//
//    @NotNull
// public static Function<ReactiveClassicGenericQueryExecutor, Future<Boolean>> deactivateStickyActionsQ(UUID stickyId) {
//        return t -> deactivateActionsWhere(STICKY_ACTION.STICKY_ID.eq(stickyId)).apply(t)
//                .map(i -> i > 0);
//    }

    @NotNull
    private static Function<DSLContext, UpdateConditionStep<Record>> deactivateStickyLocationsWhere(Condition condition) {
        return c -> c.update(STICKY_LOCATION)
                .set(STICKY_LOCATION.ACTIVE, false)
                .from(STICKY_LOCATION)
                .where(condition);
    }

    @NotNull
    private static Function<DSLContext, UpdateConditionStep<Record>> activateStickyLocationsWhere(Condition condition) {
        return c -> c.update(STICKY_LOCATION).set(STICKY_LOCATION.ACTIVE, true)
                .from(STICKY_LOCATION).where(condition);
    }

    @NotNull
    private static Function<ReactiveClassicGenericQueryExecutor, Future<List<Action>>> getActionsForSticky(UUID stickyId) {
        final var roles = field("roles", UUID[].class);
        return findMany(c -> c.select(STICKY_ACTION.ACTION_ID, roles, STICKY_ACTION.NAME, STICKY_ACTION.STICKY_ID, STICKY_ACTION.DESCRIPTION, STICKY_ACTION.ACTIVE)
                        .from(STICKY_ACTION,
                                lateral(select(arrayAggDistinct(STICKY_ACTION_ROLE.ROLE_ID).as(roles))
                                        .from(STICKY_ACTION_ROLE)
                                        .where(STICKY_ACTION.ACTION_ID.eq(STICKY_ACTION_ROLE.ACTION_ID))))
                        .where(STICKY_ACTION.STICKY_ID.eq(stickyId)).and(STICKY_ACTION.ACTIVE.eq(true)),
                rowToAction(roles));
    }

    @NotNull
    private static Function<ReactiveClassicGenericQueryExecutor, Future<List<Action>>> getActionsForStickyLocation(UUID stickyId, UUID locationId) {
        final var roles = field("roles", UUID[].class);
        return findMany(c -> c.select(STICKY_ACTION.ACTION_ID, roles, STICKY_ACTION.NAME, STICKY_ACTION.STICKY_ID, STICKY_ACTION.DESCRIPTION, STICKY_ACTION.ACTIVE, TICKET.TICKET_ID)
                        .from(STICKY_ACTION,
                                lateral(select(arrayAggDistinct(STICKY_ACTION_ROLE.ROLE_ID).as(roles))
                                        .from(STICKY_ACTION_ROLE)
                                        .where(STICKY_ACTION.ACTION_ID.eq(STICKY_ACTION_ROLE.ACTION_ID))))
                        .leftJoin(TICKET).on(TICKET.LOCATION_ID.eq(locationId).and(TICKET.ACTION_ID.eq(STICKY_ACTION.ACTION_ID)).and(TICKET.STATE.in(List.of(State.PENDING, State.ACQUIRED))))
                        .where(STICKY_ACTION.STICKY_ID.eq(stickyId)).and(STICKY_ACTION.ACTIVE.eq(true)),
                rowToAction(roles));
    }

    @NotNull
    private static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addStickyActionsQ(List<Action> actions) {
        if (actions.isEmpty()) {
            return t -> succeededFuture();
        }
        return t -> addStickyActionsTQ(actions).apply(t)
                .flatMap(ignore -> addStickyActionsRolesQ(actions).apply(t));
    }

    @NotNull
    private static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addStickyActionsTQ(List<Action> actions) {
        return execute(c -> {
            final var insert = c.insertInto(STICKY_ACTION, STICKY_ACTION.ACTION_ID, STICKY_ACTION.STICKY_ID, STICKY_ACTION.NAME, STICKY_ACTION.DESCRIPTION);
            actions.forEach(action -> insert.values(action.getId(), action.getStickyId(), action.getName(), action.getDescription()));
            return insert;
        }, actions.size(), "insert actions");
    }

    @NotNull
    private static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addStickyActionsRolesQ(List<Action> actions) {
        if (actions.isEmpty()) {
            return t -> succeededFuture();
        }
        final var actionsRoles = actions.stream()
                .flatMap(action -> action.getRoles().stream().map(role -> Pair.of(action.getId(), role)))
                .collect(Collectors.toList());
        return execute(c -> {
            final var insert = c.insertInto(STICKY_ACTION_ROLE, STICKY_ACTION_ROLE.ACTION_ID, STICKY_ACTION_ROLE.ROLE_ID);
            actionsRoles.forEach(pair -> insert.values(pair.getLeft(), pair.getRight()));
            return insert;
        }, actionsRoles.size(), "insert action's roles");
    }

}
