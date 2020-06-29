package com.victor.banana.services;

import com.victor.banana.models.events.*;
import com.victor.banana.models.events.locations.*;
import com.victor.banana.models.events.messages.CreateChannelMessage;
import com.victor.banana.models.events.messages.RecvPersonnelMessage;
import com.victor.banana.models.events.messages.RecvUpdateMessage;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.personnel.PersonnelFilter;
import com.victor.banana.models.events.personnel.UpdatePersonnel;
import com.victor.banana.models.events.stickies.CreateSticky;
import com.victor.banana.models.events.stickies.ScanSticky;
import com.victor.banana.models.events.stickies.Sticky;
import com.victor.banana.models.events.stickies.UpdateSticky;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketFilter;
import com.victor.banana.models.events.tickets.TicketState;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.List;

import static com.victor.banana.utils.Constants.DeliveryOptionsConstants.LOCAL_DELIVERY;
import static com.victor.banana.utils.Constants.EventbusAddress.PERSONNEL;
import static com.victor.banana.utils.Constants.EventbusAddress.TICKETING;

@ProxyGen
@VertxGen
public interface PersonnelService {
    static PersonnelService createProxy(Vertx vertx) {
        return new PersonnelServiceVertxEBProxy(vertx, PERSONNEL, LOCAL_DELIVERY);
    }

    void createCompany(CreateCompany createCompany, Handler<AsyncResult<Company>> result);

    void createBuildingFloors(CreateBuildingFloors createBuilding, Handler<AsyncResult<BuildingFloors>> result);

    void getBuildingsForCompany(String companyId, Handler<AsyncResult<BuildingLocations>> result);

    void getFloorLocations(String buildingId, Handler<AsyncResult<FloorLocations>> result);

    void getFloors(String buildingId, Handler<AsyncResult<List<Floor>>> result);

    void getBuildings(String buildingId, Handler<AsyncResult<List<Building>>> result);

    void getUserProfile(Personnel personnel, Handler<AsyncResult<UserProfile>> result);

    void getOrElseCreatePersonnel(TokenUser user, Handler<AsyncResult<Personnel>> result);

    void receivedPersonnelMessage(RecvPersonnelMessage chatMessage);

    void createChannel(CreateChannelMessage createChannel, Handler<AsyncResult<TelegramChannel>> result);

    void getPersonnel(String personnelId, Handler<AsyncResult<Personnel>> result);

    void findPersonnel(PersonnelFilter filter, Handler<AsyncResult<List<Personnel>>> result);

    void updatePersonnel(String personnelId, UpdatePersonnel updatePersonnel, Handler<AsyncResult<Personnel>> result);

    void deletePersonnel(String personnelId, Handler<AsyncResult<Void>> result);

    void addTelegramToUserProfile(TelegramLoginData telegramLoginData, Handler<AsyncResult<UserProfile>> result);

}
