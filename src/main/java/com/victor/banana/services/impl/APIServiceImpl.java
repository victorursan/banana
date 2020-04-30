package com.victor.banana.services.impl;

import com.victor.banana.models.events.ActionSelected;
import com.victor.banana.models.events.Personnel;
import com.victor.banana.models.events.UpdatePersonnel;
import com.victor.banana.models.events.UpdateTicketState;
import com.victor.banana.models.events.locations.CreateLocation;
import com.victor.banana.models.events.locations.Location;
import com.victor.banana.models.events.roles.CreateRole;
import com.victor.banana.models.events.roles.Role;
import com.victor.banana.models.events.stickies.*;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.models.requests.*;
import com.victor.banana.models.responses.*;
import com.victor.banana.services.CartchufiService;
import com.victor.banana.services.APIService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;

import java.util.Optional;
import java.util.UUID;

import static com.victor.banana.models.events.tickets.TicketState.*;
import static java.util.stream.Collectors.toList;

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
                    .addHandlerByOperationId("deleteSticky", this::deleteSticky)
                    .addHandlerByOperationId("updateSticky", this::updateSticky);

            routerFactory.addHandlerByOperationId("actionSelected", this::actionSelected);

            routerFactory.addHandlerByOperationId("getRoles", this::getRoles)
                    .addHandlerByOperationId("addRole", this::addRole)
                    .addHandlerByOperationId("deleteRole", this::deleteRole);

            routerFactory.addHandlerByOperationId("updatePersonnelLocation", this::updatePersonnelLocation)
                    .addHandlerByOperationId("updatePersonnelRole", this::updatePersonnelRole)
                    .addHandlerByOperationId("updatePersonnelRole", this::updatePersonnelRole)
                    .addHandlerByOperationId("getPersonnel", this::getPersonnel);

            routerFactory.addFailureHandlerByOperationId("addSticky", t -> log.error(t.getBodyAsJson().toString()));

            return routerFactory.getRouter();
        });
    }

    private void deleteSticky(RoutingContext rc) {
        final var stickyId = rc.request().getParam("stickyId");
        Future.<Boolean>future(f -> cartchufiService.deleteSticky(stickyId, f))
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
    }

    private void updateSticky(RoutingContext rc) {
        final var stickyId = rc.request().getParam("stickyId");
        final var stickyReq = rc.getBodyAsJson().mapTo(UpdateStickyReq.class);
        final var stickyUpdate = UpdateSticky.builder();
        stickyReq.getMessage().ifPresent(stickyUpdate::message);
        stickyReq.getActions().ifPresent(sa -> stickyUpdate.actions(UpdateStickyCreateAction.builder()
                .add(sa.getAdd().stream()
                        .map(as -> CreateAction.builder()
                                .roleId(as.getRoleId())
                                .message(as.getAction())
                                .build())
                        .collect(toList()))
                .activate(sa.getActivate())
                .remove(sa.getRemove()).build()));
        stickyReq.getLocations().ifPresent(sl -> stickyUpdate.locations(UpdateStickyCreateLocation.builder()
                .add(sl.getAdd().stream()
                        .map(ls -> CreateLocation.builder()
                                .location(ls.getLocation())
                                .parentLocation(ls.getParentLocation())
                                .build())
                        .collect(toList()))
                .activate(sl.getActivate())
                .remove(sl.getRemove()).build()));
        Future.<Sticky>future(f -> cartchufiService.updateSticky(UUID.fromString(stickyId).toString(), stickyUpdate.build(), f))
                .map(this::stickySerializer)
                .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(500).end(t.getMessage());
                });
    }

    private void deleteLocation(RoutingContext rc) {
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
    }

    private void deleteRole(RoutingContext rc) {
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
    }

    private void scanSticky(RoutingContext rc) {
        final var stickyLocationId = rc.request().getParam("stickyLocationId");
        Future.<StickyLocation>future(c -> cartchufiService.getStickyLocation(stickyLocationId, c))
                .map(sticky -> StickyLocationResp.builder()
                        .id(sticky.getId())
                        .message(sticky.getMessage())
                        .actions(sticky.getActions().stream().map(a -> ActionStickyResp.builder()
                                .id(a.getId())
                                .roleId(a.getRoleId())
                                .message(a.getMessage())
                                .build()).collect(toList()))
                        .locationId(sticky.getLocationId())
                        .build())
                .onSuccess(res -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(res)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(400).end(t.getMessage());
                });
    }

    private void getTicket(RoutingContext rc) {
        final var ticketId = rc.request().getParam("ticketId");
        Future.<Ticket>future(f -> cartchufiService.getTicket(ticketId, f))
                .map(this::ticketSerializer)
                .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(500).end(t.getMessage());
                });
    }

    private void updateTicket(RoutingContext rc) {
        final var ticketId = rc.request().getParam("ticketId");
        final var updateTicketReq = rc.getBodyAsJson().mapTo(UpdateTicketReq.class);

        Future.<Ticket>future(f -> {
            final var updateTicketState = UpdateTicketState.builder()
                    .ticketId(UUID.fromString(ticketId))
                    .newTicketState(ticketStateDeserializer(updateTicketReq.getNewState()).orElseThrow()) //todo
                    //todo figure out .personnelId()
                    .personnelId(UUID.fromString("cf338d20-073a-4f28-ad68-a104d02eef9d"))
                    .build();
            cartchufiService.updateTicketState(updateTicketState, f);
        })
                .map(this::ticketSerializer)
                .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(500).end(t.getMessage());
                });
    }

    private void getTickets(RoutingContext rc) {
        Future.future(cartchufiService::getTickets)
                .map(f -> f.stream().map(this::ticketSerializer).collect(toList()))
                .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(500).end(t.getMessage());
                });
    }

    private void getLocations(RoutingContext rc) {
        Future.future(cartchufiService::getLocations)
                .map(l -> l.stream().map(this::locationSerializer).collect(toList()))
                .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(500).end(t.getMessage());
                });
    }

    private void getRoles(RoutingContext rc) {
        Future.future(cartchufiService::getRoles)
                .map(l -> l.stream().map(this::roleSerializer).collect(toList()))
                .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(500).end(t.getMessage());
                });
    }

    private void actionSelected(RoutingContext rc) {
        final var actionSelectedReq = rc.getBodyAsJson().mapTo(ActionSelectedReq.class);
        final var actionSelected = ActionSelected.builder()
                .actionId(actionSelectedReq.getActionId())
                .locationId(actionSelectedReq.getLocationId())
                .build();
        Future.<Ticket>future(t -> cartchufiService.actionSelected(actionSelected, t))
                .map(this::ticketSerializer)
                .onSuccess(res -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(res)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(500).end(t.getMessage());
                });
    }

    private TicketResp ticketSerializer(Ticket t) {
        return TicketResp.builder()
                .ticketId(t.getId())
                .message(t.getMessage())
                .state(ticketStateSerializer(t.getState()))
                .build();
    }

    private String ticketStateSerializer(TicketState ticketState) {
        return switch (ticketState) {
            case ACQUIRED -> "Acquired";
            case SOLVED -> "Solved";
            case PENDING -> "Pending";
        };
    }

    private Optional<TicketState> ticketStateDeserializer(String ticketState) {
        return switch (ticketState) {
            case "Acquired" -> Optional.of(ACQUIRED);
            case "Solved" -> Optional.of(SOLVED);
            case "Pending" -> Optional.of(PENDING);
            default -> Optional.empty();
        };
    }

    private void addLocation(RoutingContext rc) {
        final var locationReq = rc.getBodyAsJson().mapTo(AddLocationReq.class);
        Future.<Location>future(c -> {
            final var createLocation = CreateLocation.builder()
                    .parentLocation(locationReq.getParentLocation())
                    .location(locationReq.getLocation())
                    .build();
            cartchufiService.createLocation(createLocation, c);
        }).map(this::locationSerializer)
                .onSuccess(l -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(l)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(400).end(t.getMessage());
                });
    }

    private void addRole(RoutingContext rc) {
        final var roleReq = rc.getBodyAsJson().mapTo(AddRoleReq.class);
        Future.<Role>future(c -> {
            final var createRole = CreateRole.builder()
                    .type(roleReq.getType())
                    .build();
            cartchufiService.createRole(createRole, c);
        }).map(this::roleSerializer)
                .onSuccess(l -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(l)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(400).end(t.getMessage());
                });
    }

    private void addSticky(RoutingContext rc) {
        final var stickyReq = rc.getBodyAsJson().mapTo(AddStickyReq.class);
        Future.<Sticky>future(c -> {
            final var createSticky = CreateSticky.builder()
                    .message(stickyReq.getMessage())
                    .actions(stickyReq.getActions().stream()
                            .map(asr -> CreateAction.builder()
                                    .message(asr.getAction())
                                    .roleId(asr.getRoleId())
                                    .build())
                            .collect(toList()))
                    .locations(stickyReq.getLocations().stream()
                            .map(lsr -> CreateLocation.builder()
                                    .location(lsr.getLocation())
                                    .parentLocation(lsr.getParentLocation())
                                    .build())
                            .collect(toList())
                    )
                    .build();
            cartchufiService.createSticky(createSticky, c);
        }).map(this::stickySerializer)
                .onSuccess(res -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(res)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(400).end(t.getMessage());
                });
    }

    private StickyResp stickySerializer(Sticky sticky) {
        return StickyResp.builder()
                .id(sticky.getId())
                .message(sticky.getMessage())
                .actions(sticky.getActions().stream().map(a -> ActionStickyResp.builder()
                        .id(a.getId())
                        .roleId(a.getRoleId())
                        .message(a.getMessage())
                        .build()).collect(toList()))
                .locations(sticky.getLocations().stream().map(this::locationSerializer).collect(toList()))
                .build();
    }

    private LocationResp locationSerializer(Location location) {
        return LocationResp.builder()
                .id(location.getId())
                .parentLocation(location.getParentLocation())
                .message(location.getText())
                .build();
    }

    private RoleResp roleSerializer(Role role) {
        return RoleResp.builder()
                .id(role.getId())
                .role(role.getType())
                .build();
    }

    private void getPersonnel(RoutingContext rc) {
        final var personnelId = rc.request().getParam("personnelId");
        Future.<Personnel>future(f -> cartchufiService.getPersonnel(personnelId, f))
                .onSuccess(l -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(l)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(400).end(t.getMessage());
                });
    }

    private void updatePersonnelRole(RoutingContext rc) {
        final var personnelId = rc.request().getParam("personnelId");
        final var updatePersRole = rc.getBodyAsJson().mapTo(UpdatePersonnelRoleReq.class).getNewRole();
        final var updatePers = UpdatePersonnel.builder().roleId(updatePersRole).build();
        Future.<Personnel>future(f -> cartchufiService.updatePersonnel(personnelId, updatePers, f))
                .onSuccess(l -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(l)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(400).end(t.getMessage());
                });
    }

    private void updatePersonnelLocation(RoutingContext rc) {
        final var personnelId = rc.request().getParam("personnelId");
        final var updatePersLoc = rc.getBodyAsJson().mapTo(UpdatePersonnelLocationReq.class).getNewLocation();
        final var updatePers = UpdatePersonnel.builder().locationId(updatePersLoc).build();
        Future.<Personnel>future(f -> cartchufiService.updatePersonnel(personnelId, updatePers, f))
                .onSuccess(l -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(l)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(400).end(t.getMessage());
                });

    }
}
