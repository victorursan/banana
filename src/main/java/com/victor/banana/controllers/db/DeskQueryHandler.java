package com.victor.banana.controllers.db;

import com.victor.banana.models.events.desk.Desk;
import com.victor.banana.models.events.desk.DeskFilter;
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor;
import io.vertx.core.Future;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Record4;
import org.jooq.SelectJoinStep;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static com.victor.banana.controllers.db.QueryHandler.*;
import static com.victor.banana.controllers.db.RowMappers.rowToDesk;
import static com.victor.banana.jooq.Tables.DESK;
import static com.victor.banana.jooq.Tables.FLOOR;

public final class DeskQueryHandler {
    private DeskQueryHandler() {
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addDeskQ(Desk desk) {
        return execute(insertIntoDesk(desk), 1, "add desk");
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<Desk>>> getDesksForBuildingIdQ(UUID buildingId) {
        return findMany(selectWhere(selectDesk().andThen(c -> c.innerJoin(FLOOR).using(DESK.FLOOR_ID)), FLOOR.BUILDING_ID.eq(buildingId)),
                rowToDesk());
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<Desk>>> getDesksQ(DeskFilter filter) {
        return getDesksForBuildingIdQ(filter.getBuildingId());
    }

    @NotNull
    private static Function<DSLContext, SelectJoinStep<Record4<UUID, String, UUID, Boolean>>> selectDesk() {
        return c -> c.select(DESK.DESK_ID, DESK.NAME, DESK.FLOOR_ID, DESK.ACTIVE)
                .from(DESK);
    }

    @NotNull
    private static Function<DSLContext, Query> insertIntoDesk(Desk desk) {
        return c -> c.insertInto(DESK, DESK.DESK_ID, DESK.NAME, DESK.FLOOR_ID, DESK.ACTIVE)
                .values(desk.getId(), desk.getName(), desk.getFloorId(), desk.getActive());
    }
}
