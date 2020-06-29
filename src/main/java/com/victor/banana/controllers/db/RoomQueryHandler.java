package com.victor.banana.controllers.db;

import com.victor.banana.jooq.enums.RoomType;
import com.victor.banana.models.events.desk.Desk;
import com.victor.banana.models.events.desk.DeskFilter;
import com.victor.banana.models.events.locations.*;
import com.victor.banana.models.events.room.Room;
import com.victor.banana.models.events.room.RoomFilter;
import com.victor.banana.utils.CallbackUtils;
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor;
import io.vertx.core.Future;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static com.victor.banana.controllers.db.QueryHandler.*;
import static com.victor.banana.controllers.db.RowMappers.*;
import static com.victor.banana.jooq.Tables.*;
import static com.victor.banana.jooq.tables.Room.ROOM;
import static io.vertx.core.Future.succeededFuture;

public final class RoomQueryHandler {
    private RoomQueryHandler() {
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addRoomQ(Room room) {
        return execute(insertIntoRoom(room), 1, "add room");
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<Room>>> getRoomsForBuildingIdQ(UUID buildingId) {
        return findMany(selectWhere(selectRoom().andThen(c -> c.innerJoin(FLOOR).using(ROOM.FLOOR_ID)), FLOOR.BUILDING_ID.eq(buildingId)),
                rowToRoom());
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<Room>>> getRoomsQ(RoomFilter filter) {
        return getRoomsForBuildingIdQ(filter.getBuildingId());
    }

    @NotNull
    private static Function<DSLContext, SelectJoinStep<Record6<UUID, String, UUID, RoomType, Integer, Boolean>>> selectRoom() {
        return c -> c.select(ROOM.ROOM_ID, ROOM.NAME, ROOM.FLOOR_ID, ROOM.ROOM_TYPE, ROOM.CAPACITY, ROOM.ACTIVE)
                .from(ROOM);
    }

    @NotNull
    private static Function<DSLContext, Query> insertIntoRoom(Room room) {
        return c -> c.insertInto(ROOM, ROOM.ROOM_ID, ROOM.NAME, ROOM.FLOOR_ID, ROOM.ROOM_TYPE, ROOM.CAPACITY, ROOM.ACTIVE)
                .values(room.getId(), room.getName(), room.getFloorId(), roomTypeToDbRoomType(room.getRoomType()), room.getCapacity(), room.getActive());
    }
}
