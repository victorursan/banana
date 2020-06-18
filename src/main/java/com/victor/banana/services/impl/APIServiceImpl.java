package com.victor.banana.services.impl;

import com.victor.banana.models.events.TelegramLoginData;
import com.victor.banana.models.events.UserProfile;
import com.victor.banana.models.events.locations.BuildingFloors;
import com.victor.banana.models.events.locations.BuildingLocations;
import com.victor.banana.models.events.locations.Company;
import com.victor.banana.models.events.locations.FloorLocations;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.personnel.PersonnelFilter;
import com.victor.banana.models.events.stickies.ScanSticky;
import com.victor.banana.models.events.stickies.Sticky;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketFilter;
import com.victor.banana.models.requests.*;
import com.victor.banana.models.responses.TicketResp;
import com.victor.banana.services.APIService;
import com.victor.banana.services.CartchufiService;
import com.victor.banana.utils.Constants.PersonnelRole;
import com.victor.banana.utils.SecurityUtils;
import com.victor.banana.utils.SecurityUtils.Authority;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;

import java.util.*;

import static com.victor.banana.utils.MappersHelper.mapTsF;
import static com.victor.banana.utils.MappersHelper.mapperToFuture;
import static com.victor.banana.utils.ReqMapper.*;
import static com.victor.banana.utils.RespMapper.*;
import static com.victor.banana.utils.SecurityUtils.Authority.*;

public class APIServiceImpl implements APIService {
    private static final Logger log = LoggerFactory.getLogger(APIServiceImpl.class);
    private final CartchufiService cartchufiService;

    public APIServiceImpl(CartchufiService cartchufiService) {
        this.cartchufiService = cartchufiService;
    }

    @Override
    public final Future<Router> routes(Future<OpenAPI3RouterFactory> routerFactoryFuture) {
        return routerFactoryFuture.map(routerFactory -> {
            //todo add failures handler
            //locations
            routerFactory.addHandlerByOperationId("getBuildingsForCompany", this::getBuildingsForCompany)
                    .addHandlerByOperationId("getFloorLocations", this::getFloorForBuilding)
                    .addHandlerByOperationId("addCompany", this::addCompany)
                    .addHandlerByOperationId("addBuildingFloors", this::addBuildingFloors);

            //tickets
            routerFactory.addHandlerByOperationId("getTicket", this::getTicket)
                    .addHandlerByOperationId("getTickets", this::getTickets)
                    .addHandlerByOperationId("updateTicket", this::updateTicket)
                    .addHandlerByOperationId("actionSelected", this::actionSelected);

            //stickies
            routerFactory.addHandlerByOperationId("addSticky", this::addSticky)
                    .addHandlerByOperationId("scanSticky", this::scanSticky)
                    .addHandlerByOperationId("getStickies", this::getStickies)
                    .addHandlerByOperationId("getSticky", this::getSticky)
                    .addHandlerByOperationId("updateSticky", this::updateSticky);

            //roles
            routerFactory.addHandlerByOperationId("getRoles", this::getRoles); //todo figure out how to make static handler

            //personnel
            routerFactory.addHandlerByOperationId("updatePersonnel", this::updatePersonnel)
                    .addHandlerByOperationId("getPersonnel", this::getPersonnel)
                    .addHandlerByOperationId("getPersonnelByType", this::getPersonnelByType);

            //
            routerFactory.addHandlerByOperationId("getProfile", this::getProfile)
                    .addHandlerByOperationId("addTelegramToUser", this::addTelegramToUser)
                    .addHandlerByOperationId("deleteUserProfile", this::deleteUserProfile);

            return routerFactory.getRouter();
        });
    }

    // locations
    private void addCompany(RoutingContext rc) {
        isUserAuthorized(rc, ADMIN, personnel -> {
            final var addCompanyReq = rc.getBodyAsJson().mapTo(AddCompanyReq.class);
            final var createCompany = createCompanyDeserializer().apply(addCompanyReq);
            Future.<Company>future(r -> cartchufiService.createCompany(createCompany, r))
                    .flatMap(mapperToFuture(companyBuildingSerializer()))
                    .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 500));
        });
    }

    private void addBuildingFloors(RoutingContext rc) {
        isUserAuthorized(rc, ADMIN, personnel -> {
            final var addBuildingFloorsReq = rc.getBodyAsJson().mapTo(AddBuildingFloorsReq.class);
            final var createBuildingFloors = createBuildingFloorsDeserializer().apply(addBuildingFloorsReq);
            Future.<BuildingFloors>future(r -> cartchufiService.createBuildingFloors(createBuildingFloors, r))
                    .flatMap(mapperToFuture(buildingFloorsSerializer()))
                    .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 500));
        });
    }

    private void getBuildingsForCompany(RoutingContext rc) {
        isUserAuthorized(rc, COMMUNITY, personnel -> {
                    final var companyId = rc.request().getParam("companyId");
                    Future.<BuildingLocations>future(r -> cartchufiService.getBuildingsForCompany(UUID.fromString(companyId).toString(), r))
                            .flatMap(mapperToFuture(buildingLocationsSerializer()))
                            .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                            .onFailure(failureHandler(rc, 500));
                }
        );
    }

    private void getFloorForBuilding(RoutingContext rc) {
        isUserAuthorized(rc, MEMBER, personnel ->
                Future.<FloorLocations>future(r -> cartchufiService.getFloorLocations(personnel.getBuildingId().toString(), r))
                        .flatMap(mapperToFuture(floorLocationsSerializer()))
                        .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                        .onFailure(failureHandler(rc, 500))
        );
    }


    // user profile
    private void addTelegramToUser(RoutingContext rc) {
        isUserAuthorized(rc, COMMUNITY, personnel -> {
            final var telegramReq = rc.getBodyAsJson().mapTo(TelegramLoginDataReq.class);
            final var telegramLogin = telegramLoginDataDeserializer(personnel).apply(telegramReq);
            Future.<UserProfile>future(f -> cartchufiService.addTelegramToUserProfile(telegramLogin, f))
                    .map(userProfileSerializer())
                    .onSuccess(res -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 500));
        });
    }

    private void deleteUserProfile(RoutingContext rc) {
        isUserAuthorized(rc, MEMBER, personnel -> {
            Future.<Void>future(f -> cartchufiService.deletePersonnel(personnel.getId().toString(), f))
                    .onSuccess(res -> rc.response().setStatusCode(204).end())
                    .onFailure(failureHandler(rc, 500));
        });
    }

    private void getProfile(RoutingContext rc) {
        isUserAuthorized(rc, MEMBER, personnel -> {
            Future.<UserProfile>future(f -> cartchufiService.getUserProfile(personnel, f))
                    .map(userProfileSerializer())
                    .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 500));
        });
    }

    //tickets
    private void getTicket(RoutingContext rc) {
        isUserAuthorized(rc, MEMBER, personnel -> {
            final var ticketId = rc.request().getParam("ticketId");
            Future.<Ticket>future(f -> cartchufiService.getTicket(ticketId, f))
                    .map(ticketSerializer())
                    .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 500));
        });
    }

    private void updateTicket(RoutingContext rc) {
        isUserAuthorized(rc, COMMUNITY, personnel -> {
            final var ticketId = rc.request().getParam("ticketId");
            final var updateTicketReq = rc.getBodyAsJson().mapTo(UpdateTicketReq.class);
            Future.<Ticket>future(f -> {
                final var updateTicketState = updateTicketStateDeserializer(personnel).apply(updateTicketReq);
                cartchufiService.updateTicketState(UUID.fromString(ticketId).toString(), updateTicketState, f);
            })
                    .map(ticketSerializer())
                    .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 500));
        });
    }

    private void getTickets(RoutingContext rc) {
        final var isUser = !rc.queryParam("user").isEmpty() && Boolean.parseBoolean(rc.queryParam("user").get(0));
        isUserAuthorized(rc, isUser ? MEMBER : COMMUNITY, personnel ->
                Future.<List<Ticket>>future(f ->
                        cartchufiService.getTickets(TicketFilter.builder().forUser(isUser ? Optional.of(personnel.getId()) : Optional.empty()).build(), f))
                        .flatMap(mapTsF(ticketSerializer(!isUser), Comparator.comparing(TicketResp::getCreatedAt).reversed()))
                        .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                        .onFailure(failureHandler(rc, 500)));
    }

    private void actionSelected(RoutingContext rc) {
        isUserAuthorized(rc, MEMBER, personnel -> {
            final var actionSelectedReq = rc.getBodyAsJson().mapTo(ActionSelectedReq.class);
            final var actionSelected = actionSelectedDeserializer().apply(actionSelectedReq);
            Future.<Ticket>future(t -> cartchufiService.actionSelected(personnel, actionSelected, t))
                    .map(ticketSerializer())
                    .onSuccess(res -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 400));
        });
    }


    //roles
    private void getRoles(RoutingContext rc) {
        isUserAuthorized(rc, COMMUNITY, personnel -> {
            final var roles = Arrays.stream(PersonnelRole.values())
                    .map(roleSerializer());
            rc.response().setStatusCode(200).end(Json.encodeToBuffer(roles));
        });
    }

    //stickies
    private void addSticky(RoutingContext rc) {
        isUserAuthorized(rc, COMMUNITY, personnel -> {
            final var stickyReq = rc.getBodyAsJson().mapTo(AddStickyReq.class);
            Future.<Sticky>future(c -> {
                final var createSticky = createStickyDeserializer().apply(stickyReq);
                cartchufiService.createSticky(createSticky, c);
            }).map(stickySerializer())
                    .onSuccess(res -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 400));
        });
    }

    private void scanSticky(RoutingContext rc) {
        isUserAuthorized(rc, MEMBER, personnel -> {
            final var stickyLocationId = rc.request().getParam("stickyLocationId");
            Future.<ScanSticky>future(c -> cartchufiService.getScanSticky(stickyLocationId, c))
                    .map(scanStickySerializer())
                    .onSuccess(res -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 400));
        });
    }

    private void getStickies(RoutingContext rc) {
        isUserAuthorized(rc, COMMUNITY, personnel ->
                Future.future(cartchufiService::getStickies)
                        .flatMap(mapTsF(stickySerializer()))
                        .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                        .onFailure(failureHandler(rc, 400)));
    }

    private void getSticky(RoutingContext rc) {
        isUserAuthorized(rc, COMMUNITY, personnel -> {
            final var stickyId = rc.request().getParam("stickyId");
            Future.<Sticky>future(f -> cartchufiService.getSticky(stickyId, f))
                    .map(stickySerializer())
                    .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 400));
        });
    }

    private void updateSticky(RoutingContext rc) {
        isUserAuthorized(rc, COMMUNITY, personnel -> {
            final var stickyId = rc.request().getParam("stickyId");
            final var stickyReq = rc.getBodyAsJson().mapTo(UpdateStickyReq.class);
            final var stickyUpdate = updateStickyDeserializer().apply(stickyReq);
            Future.<Sticky>future(f -> cartchufiService.updateSticky(UUID.fromString(stickyId).toString(), stickyUpdate, f))
                    .map(stickySerializer())
                    .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 500));
        });
    }

    //personnel
    private void getPersonnel(RoutingContext rc) {
        isUserAuthorized(rc, MEMBER, personnel -> {
            final var personnelId = rc.request().getParam("personnelId");
            Future.<Personnel>future(f -> cartchufiService.getPersonnel(personnelId, f))
                    .map(personnelSerializer())
                    .onSuccess(l -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(l)))
                    .onFailure(failureHandler(rc, 400));
        });
    }

    private void getPersonnelByType(RoutingContext rc) {
        isUserAuthorized(rc, COMMUNITY, personnel -> {
            final var personnelFilter = PersonnelFilter.builder();
            if (!rc.queryParam("operating").isEmpty()) {
                personnelFilter.operating(Boolean.parseBoolean(rc.queryParam("operating").get(0)));
            }
            if (!rc.queryParam("username").isEmpty()) {
                personnelFilter.username(Optional.of(rc.queryParam("username").get(0)));
            }

            Future.<List<Personnel>>future(f -> cartchufiService.findPersonnel(personnelFilter.build(), f))
                    .flatMap(mapTsF(personnelSerializer()))
                    .onSuccess(l -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(l)))
                    .onFailure(failureHandler(rc, 400));
        });
    }

    private void updatePersonnel(RoutingContext rc) {
        isUserAuthorized(rc, ADMIN, personnel -> {
            final var personnelId = rc.request().getParam("personnelId");
            final var updatePersonnelReq = rc.getBodyAsJson().mapTo(UpdatePersonnelReq.class);
            final var updatePersonnel = updatePersonnelDeserializer().apply(updatePersonnelReq);
            Future.<Personnel>future(f -> cartchufiService.updatePersonnel(personnelId, updatePersonnel, f))
                    .map(personnelSerializer())
                    .onSuccess(l -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(l)))
                    .onFailure(failureHandler(rc, 400));
        });
    }

    private void isUserAuthorized(RoutingContext rc, Authority authority, Handler<Personnel> authorizedPersonnel) {
        SecurityUtils.isUserAuthorized(rc.user(), authority)
                .onSuccess(tUserOpt ->
                        tUserOpt.ifPresentOrElse(tUser -> {
                            Future.<Personnel>future(p -> cartchufiService.getOrElseCreatePersonnel(tUser, p))
                                    .onSuccess(authorizedPersonnel)
                                    .onFailure(failureHandler(rc, 500));
                        }, () -> rc.response().setStatusCode(401).end())
                )
                .onFailure(failureHandler(rc, 500));
    }

    private Handler<Throwable> failureHandler(RoutingContext rc, int i) {
        return t -> {
            log.error(t.getMessage(), t);
            rc.response().setStatusCode(i).end(t.getMessage());
        };
    }

}
