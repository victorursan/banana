package com.victor.banana.services.impl;

import com.victor.banana.models.events.desk.CreateDesk;
import com.victor.banana.models.events.desk.Desk;
import com.victor.banana.models.events.desk.DeskFilter;
import com.victor.banana.models.events.room.CreateRoom;
import com.victor.banana.models.events.room.Room;
import com.victor.banana.models.events.room.RoomFilter;
import com.victor.banana.services.BookingService;
import com.victor.banana.services.DatabaseService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.util.List;

import static com.victor.banana.utils.CreateMappers.createDeskToDesk;
import static com.victor.banana.utils.CreateMappers.createRoomToRoom;

public class BookingServiceImpl implements BookingService {
    private final DatabaseService dbService;

    public BookingServiceImpl(DatabaseService dbService) {
        this.dbService = dbService;
    }

    @Override
    public final void createDesk(CreateDesk createDesk, Handler<AsyncResult<Desk>> result) {
        Future.<Desk>future(c -> dbService.addDesk(createDeskToDesk().apply(createDesk), c))
                .onComplete(result);
    }

    @Override
    public final void createRoom(CreateRoom createRoom, Handler<AsyncResult<Room>> result) {
        Future.<Room>future(c -> dbService.addRoom(createRoomToRoom().apply(createRoom), c))
                .onComplete(result);
    }

    @Override
    public final void findDesks(DeskFilter filter, Handler<AsyncResult<List<Desk>>> result) {
        Future.<List<Desk>>future(c -> dbService.findDesks(filter, c))
                .onComplete(result);
    }

    @Override
    public final void findRooms(RoomFilter filter, Handler<AsyncResult<List<Room>>> result) {
        Future.<List<Room>>future(c -> dbService.findRooms(filter, c))
                .onComplete(result);
    }

}
