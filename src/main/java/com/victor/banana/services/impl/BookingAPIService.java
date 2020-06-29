package com.victor.banana.services.impl;

import com.victor.banana.models.events.desk.Desk;
import com.victor.banana.models.events.desk.DeskFilter;
import com.victor.banana.models.events.room.Room;
import com.victor.banana.models.events.room.RoomFilter;
import com.victor.banana.models.requests.booking.AddDeskReq;
import com.victor.banana.models.requests.booking.AddRoomReq;
import com.victor.banana.services.APIService;
import com.victor.banana.services.BookingService;
import com.victor.banana.services.PersonnelService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;

import java.util.List;

import static com.victor.banana.utils.MappersHelper.mapTsF;
import static com.victor.banana.utils.MappersHelper.mapperToFuture;
import static com.victor.banana.utils.ReqMapper.createDeskDeserializer;
import static com.victor.banana.utils.ReqMapper.createRoomDeserializer;
import static com.victor.banana.utils.RespMapper.deskSerializer;
import static com.victor.banana.utils.RespMapper.roomSerializer;
import static com.victor.banana.utils.SecurityUtils.Authority.ADMIN;
import static com.victor.banana.utils.SecurityUtils.Authority.MEMBER;
import static io.vertx.core.json.Json.encodeToBuffer;

public class BookingAPIService extends APIService {
    private final BookingService bookingService;

    public BookingAPIService(BookingService bookingService, PersonnelService personnelService) {
        super(personnelService);
        this.bookingService = bookingService;
    }

    @Override
    public final Future<Router> routes(Future<OpenAPI3RouterFactory> routerFactoryFuture) {
        return routerFactoryFuture.map(routerFactory -> {
            //desks
            routerFactory.addHandlerByOperationId("getDesks", this::getDesks)
                    .addHandlerByOperationId("addDesk", this::addDesk);
            //rooms
            routerFactory.addHandlerByOperationId("getRooms", this::getRooms)
                    .addHandlerByOperationId("addRoom", this::addRoom);


            return routerFactory.getRouter();
        });
    }

    // desks
    private void getDesks(RoutingContext rc) {
        isUserAuthorized(rc, MEMBER, personnel -> {
            final var buildingId = personnel.getBuildingId().orElseThrow();//todo
            Future.<List<Desk>>future(r -> bookingService.findDesks(DeskFilter.builder().buildingId(buildingId).build(), r))
                    .flatMap(mapTsF(deskSerializer()))
                    .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 500));
        });
    }

    //
    private void addDesk(RoutingContext rc) {
        isUserAuthorized(rc, ADMIN, personnel -> {
            final var addDeskReq = rc.getBodyAsJson().mapTo(AddDeskReq.class);
            final var createDesk = createDeskDeserializer().apply(addDeskReq);
            Future.<Desk>future(r -> bookingService.createDesk(createDesk, r))
                    .flatMap(mapperToFuture(deskSerializer()))
                    .onSuccess(res -> rc.response().setStatusCode(201).end(encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 500));
        });
    }

    // rooms

    private void getRooms(RoutingContext rc) {
        isUserAuthorized(rc, MEMBER, personnel -> {
            final var buildingId = personnel.getBuildingId().orElseThrow();//todo
            Future.<List<Room>>future(r -> bookingService.findRooms(RoomFilter.builder().buildingId(buildingId).build(), r))
                    .flatMap(mapTsF(roomSerializer()))
                    .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 500));
        });
    }

    //
    private void addRoom(RoutingContext rc) {
        isUserAuthorized(rc, MEMBER, personnel -> {
            final var addRoomReq = rc.getBodyAsJson().mapTo(AddRoomReq.class);
            final var createRoom = createRoomDeserializer().apply(addRoomReq);
            Future.<Room>future(r -> bookingService.createRoom(createRoom, r))
                    .flatMap(mapperToFuture(roomSerializer()))
                    .onSuccess(res -> rc.response().setStatusCode(201).end(encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 500));
        });
    }


}
