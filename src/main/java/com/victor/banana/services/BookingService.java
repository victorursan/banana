package com.victor.banana.services;

import com.victor.banana.models.events.desk.CreateDesk;
import com.victor.banana.models.events.desk.Desk;
import com.victor.banana.models.events.desk.DeskFilter;
import com.victor.banana.models.events.room.CreateRoom;
import com.victor.banana.models.events.room.Room;
import com.victor.banana.models.events.room.RoomFilter;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.List;

import static com.victor.banana.utils.Constants.DeliveryOptionsConstants.LOCAL_DELIVERY;
import static com.victor.banana.utils.Constants.EventbusAddress.BOOKING;

@ProxyGen
@VertxGen
public interface BookingService {

    static BookingService createProxy(Vertx vertx) {
        return new BookingServiceVertxEBProxy(vertx, BOOKING, LOCAL_DELIVERY);
    }

    void createDesk(CreateDesk createSticky, Handler<AsyncResult<Desk>> result);

    void createRoom(CreateRoom createRoom, Handler<AsyncResult<Room>> result);

    void findDesks(DeskFilter filter, Handler<AsyncResult<List<Desk>>> result);

    void findRooms(RoomFilter filter, Handler<AsyncResult<List<Room>>> result);

}
