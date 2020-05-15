package com.victor.banana.services.impl;

import com.victor.banana.models.events.locations.Location;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.personnel.PersonnelFilter;
import com.victor.banana.models.events.roles.Role;
import com.victor.banana.models.events.stickies.Sticky;
import com.victor.banana.models.events.stickies.StickyLocation;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketFilter;
import com.victor.banana.models.requests.*;
import com.victor.banana.services.APIService;
import com.victor.banana.services.CartchufiService;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.victor.banana.services.impl.ReqMapper.*;
import static com.victor.banana.services.impl.RespMapper.*;
import static com.victor.banana.utils.MappersHelper.mapTs;

public class APIServiceImpl implements APIService {
    private static final Logger log = LoggerFactory.getLogger(APIServiceImpl.class);
    private final CartchufiService cartchufiService;

    public APIServiceImpl(CartchufiService cartchufiService) {
        this.cartchufiService = cartchufiService;
    }

    @Override
    public final Future<Router> routes(Future<OpenAPI3RouterFactory> routerFactoryFuture) {
        return routerFactoryFuture.map(routerFactory -> {

            routerFactory.addHandlerByOperationId("getLocations", this::getLocations) //todo add failures handler
                    .addHandlerByOperationId("addLocation", this::addLocation)
                    .addHandlerByOperationId("deleteLocation", this::deleteLocation);

            routerFactory.addHandlerByOperationId("getTicket", this::getTicket)
                    .addHandlerByOperationId("getTickets", this::getTickets)
                    .addHandlerByOperationId("updateTicket", this::updateTicket);

            routerFactory.addHandlerByOperationId("addSticky", this::addSticky)
                    .addHandlerByOperationId("scanSticky", this::scanSticky)
                    .addHandlerByOperationId("getStickies", this::getStickies)
                    .addHandlerByOperationId("getSticky", this::getSticky)
                    .addHandlerByOperationId("updateSticky", this::updateSticky);

            routerFactory.addHandlerByOperationId("actionSelected", this::actionSelected);

            routerFactory.addHandlerByOperationId("getRoles", this::getRoles)
                    .addHandlerByOperationId("addRole", this::addRole)
                    .addHandlerByOperationId("deleteRole", this::deleteRole);

            routerFactory.addHandlerByOperationId("updatePersonnel", this::updatePersonnel)
                    .addHandlerByOperationId("getPersonnel", this::getPersonnel)
                    .addHandlerByOperationId("getPersonnelByType", this::getPersonnelByType);

            return routerFactory.getRouter();
        });
    }

    private void updateSticky(RoutingContext rc) {
        isUserAuthorized(rc, Authority.COMMUNITY, personnel -> {
            final var stickyId = rc.request().getParam("stickyId");
            final var stickyReq = rc.getBodyAsJson().mapTo(UpdateStickyReq.class);
            final var stickyUpdate = updateStickyDeserializer().apply(stickyReq);
            Future.<Sticky>future(f -> cartchufiService.updateSticky(UUID.fromString(stickyId).toString(), stickyUpdate, f))
                    .map(stickySerializer())
                    .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 500));
        });
    }

    private void deleteLocation(RoutingContext rc) {
        isUserAuthorized(rc, Authority.ADMIN, personnel -> {
            final var locationId = rc.request().getParam("locationId");
            Future.<Boolean>future(f -> cartchufiService.deleteLocation(UUID.fromString(locationId).toString(), f))
                    .onSuccess(b -> {
                        if (b) {
                            rc.response().setStatusCode(200).end();
                        } else {
                            rc.response().setStatusCode(404).end();
                        }
                    })
                    .onFailure(t -> {
                        log.error("Something went wrong", t);
                        rc.response().setStatusCode(500).end();
                    });
        });
    }

    private void deleteRole(RoutingContext rc) {
        isUserAuthorized(rc, Authority.ADMIN, personnel -> {
            final var roleId = rc.request().getParam("roleId");
            Future.<Boolean>future(f -> cartchufiService.deleteRole(roleId, f))
                    .onSuccess(b -> {
                        if (b) {
                            rc.response().setStatusCode(200).end();
                        } else {
                            rc.response().setStatusCode(404).end();
                        }
                    })
                    .onFailure(t -> {
                        log.error("Something went wrong", t);
                        rc.response().setStatusCode(500).end();
                    });
        });
    }

    private void scanSticky(RoutingContext rc) {
        isUserAuthorized(rc, Authority.MEMBER, personnel -> {
            final var stickyLocationId = rc.request().getParam("stickyLocationId");
            Future.<StickyLocation>future(c -> cartchufiService.getStickyLocation(stickyLocationId, c))
                    .map(stickyLocationSerializer())
                    .onSuccess(res -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 400));
        });
    }

    private void getTicket(RoutingContext rc) {
        isUserAuthorized(rc, Authority.MEMBER, personnel -> {
            final var ticketId = rc.request().getParam("ticketId");
            Future.<Ticket>future(f -> cartchufiService.getTicket(ticketId, f))
                    .map(ticketSerializer())
                    .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 500));
        });
    }

    private void updateTicket(RoutingContext rc) {
        isUserAuthorized(rc, Authority.COMMUNITY, personnel -> {
            final var ticketId = rc.request().getParam("ticketId");
            final var updateTicketReq = rc.getBodyAsJson().mapTo(UpdateTicketReq.class);
            Future.<Ticket>future(f -> {
                final var updateTicketState = updateTicketStateDeserializer().apply(updateTicketReq);
                cartchufiService.updateTicketState(UUID.fromString(ticketId).toString(), updateTicketState, f);
            })
                    .map(ticketSerializer())
                    .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 500));
        });
    }

    private void getTickets(RoutingContext rc) {
        final var isUser = !rc.queryParam("user").isEmpty() && Boolean.parseBoolean(rc.queryParam("user").get(0));
        isUserAuthorized(rc, isUser ? Authority.MEMBER : Authority.COMMUNITY, personnel ->
                Future.<List<Ticket>>future(f ->
                        cartchufiService.getTickets(TicketFilter.builder().forUser(isUser ? Optional.of(personnel.getId()) : Optional.empty()).build(), f))
                        .map(mapTs(ticketSerializer()))
                        .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                        .onFailure(failureHandler(rc, 500)));
    }

    private void getLocations(RoutingContext rc) {
        isUserAuthorized(rc, Authority.COMMUNITY, personnel ->
                Future.future(cartchufiService::getLocations)
                        .map(mapTs(locationSerializer()))
                        .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                        .onFailure(failureHandler(rc, 500)));
    }

    private void getRoles(RoutingContext rc) {
        isUserAuthorized(rc, Authority.COMMUNITY, personnel ->
                Future.future(cartchufiService::getRoles)
                        .map(mapTs(roleSerializer()))
                        .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                        .onFailure(failureHandler(rc, 500)));
    }

    private void actionSelected(RoutingContext rc) {
        isUserAuthorized(rc, Authority.MEMBER, personnel -> {
            final var actionSelectedReq = rc.getBodyAsJson().mapTo(ActionSelectedReq.class);
            final var actionSelected = actionSelectedDeserializer().apply(actionSelectedReq);
            Future.<Ticket>future(t -> cartchufiService.actionSelected(personnel, actionSelected, t))
                    .map(ticketSerializer())
                    .onSuccess(res -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 400));
        });
    }

    private void addLocation(RoutingContext rc) {
        isUserAuthorized(rc, Authority.ADMIN, personnel -> {
            final var locationReq = rc.getBodyAsJson().mapTo(AddLocationReq.class);
            Future.<Location>future(c -> {
                final var createLocation = createLocationDeserializer().apply(locationReq);
                cartchufiService.createLocation(createLocation, c);
            }).map(locationSerializer())
                    .onSuccess(l -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(l)))
                    .onFailure(failureHandler(rc, 400));
        });
    }

    private void addRole(RoutingContext rc) {
        isUserAuthorized(rc, Authority.ADMIN, personnel -> {
            final var roleReq = rc.getBodyAsJson().mapTo(AddRoleReq.class);
            Future.<Role>future(c -> {
                final var createRole = createRoleDeserializer().apply(roleReq);
                cartchufiService.createRole(createRole, c);
            }).map(roleSerializer())
                    .onSuccess(l -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(l)))
                    .onFailure(failureHandler(rc, 400));
        });
    }

    private void addSticky(RoutingContext rc) {
        isUserAuthorized(rc, Authority.COMMUNITY, personnel -> {
            final var stickyReq = rc.getBodyAsJson().mapTo(AddStickyReq.class);
            Future.<Sticky>future(c -> {
                final var createSticky = createStickyDeserializer().apply(stickyReq);
                cartchufiService.createSticky(createSticky, c);
            }).map(stickySerializer())
                    .onSuccess(res -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 400));
        });
    }

    private void getStickies(RoutingContext rc) {
        isUserAuthorized(rc, Authority.COMMUNITY, personnel ->
                Future.future(cartchufiService::getStickies)
                        .map(mapTs(stickySerializer()))
                        .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                        .onFailure(failureHandler(rc, 400)));
    }

    private void getSticky(RoutingContext rc) {
        isUserAuthorized(rc, Authority.COMMUNITY, personnel -> {
            final var stickyId = rc.request().getParam("stickyId");
            Future.<Sticky>future(f -> cartchufiService.getSticky(stickyId, f))
                    .map(stickySerializer())
                    .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                    .onFailure(failureHandler(rc, 400));
        });
    }

    private void getPersonnel(RoutingContext rc) {
        isUserAuthorized(rc, Authority.MEMBER, personnel -> {
            final var personnelId = rc.request().getParam("personnelId");
            Future.<Personnel>future(f -> cartchufiService.getPersonnel(personnelId, f))
                    .map(personnelSerializer())
                    .onSuccess(l -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(l)))
                    .onFailure(failureHandler(rc, 400));
        });
    }

    private void getPersonnelByType(RoutingContext rc) {
        isUserAuthorized(rc, Authority.COMMUNITY, personnel -> {
            final var personnelFilter = PersonnelFilter.builder();
            if (!rc.queryParam("operating").isEmpty()) {
                personnelFilter.operating(Boolean.parseBoolean(rc.queryParam("operating").get(0)));
            }
            if (!rc.queryParam("username").isEmpty()) {
                personnelFilter.username(rc.queryParam("username").get(0));
            }

            Future.<List<Personnel>>future(f -> cartchufiService.findPersonnel(personnelFilter.build(), f))
                    .map(mapTs(personnelSerializer()))
                    .onSuccess(l -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(l)))
                    .onFailure(failureHandler(rc, 400));
        });
    }

    private void updatePersonnel(RoutingContext rc) {
        isUserAuthorized(rc, Authority.ADMIN, personnel -> {
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
