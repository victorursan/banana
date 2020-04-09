package com.victor.banana.verticles;

import com.victor.banana.models.events.ActionSelected;
import com.victor.banana.models.events.CreateLocation;
import com.victor.banana.models.events.Location;
import com.victor.banana.models.events.roles.Role;
import com.victor.banana.models.events.stickies.CreateAction;
import com.victor.banana.models.events.stickies.CreateSticky;
import com.victor.banana.models.events.stickies.Sticky;
import com.victor.banana.models.events.stickies.StickyLocation;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.models.requests.ActionSelectedReq;
import com.victor.banana.models.requests.AddLocationReq;
import com.victor.banana.models.requests.AddStickyReq;
import com.victor.banana.models.responses.*;
import com.victor.banana.services.CartchufiService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

import java.util.Set;
import java.util.UUID;

import static com.victor.banana.utils.Constants.EventbusAddress.CARTCHUFI_ENGINE;
import static io.vertx.core.http.HttpHeaders.*;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static java.util.stream.Collectors.toList;


public class HttpServerVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(HttpServerVerticle.class);
    private HttpServer server;
    private CartchufiService cartchufiService;

    @Override
    public void start(Promise<Void> startPromise) {
        cartchufiService = CartchufiService.createProxy(vertx, CARTCHUFI_ENGINE);
        server = vertx.createHttpServer(new HttpServerOptions(vertx.getOrCreateContext().config()))
                .requestHandler(routes())
                .listen(l -> startPromise.handle(l.mapEmpty()));
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        server.close(stopPromise);
    }

    private Router routes() {
        final var router = Router.router(vertx);
        final var allowedHeaders = Set.of(ACCESS_CONTROL_ALLOW_ORIGIN.toString(), ORIGIN.toString(), CONTENT_TYPE.toString(), ACCEPT.toString());
        final var allowedMethods = Set.of(GET, POST);

        router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));

        router.get("/healtz").handler(healthCheck());
        router.get("/api/tickets/:ticketId").handler(this::getTicket);
        router.post("/api/stickies").handler(BodyHandler.create()).handler(this::addSticky);
        router.get("/api/stickies/:stickyLocationId").handler(this::scanSticky);
        router.post("/api/actions").handler(BodyHandler.create()).handler(this::actionSelected);
        router.get("/api/locations").handler(this::getLocations);
        router.post("/api/locations").handler(BodyHandler.create()).handler(this::addLocation);
        router.get("/api/roles").handler(this::getRoles);
        return router;
    }

    private HealthCheckHandler healthCheck() {
        return HealthCheckHandler.create(vertx)
                .register("httpServer", f -> f.complete(Status.OK()));
    }

    private void scanSticky(RoutingContext rc) {
        final var stickyLocationId = rc.request().getParam("stickyLocationId");
        Future.<StickyLocation>future(c -> cartchufiService.getStickyLocation(stickyLocationId, c))
                .map(sticky -> StickyLocationResp.builder()
                        .id(UUID.fromString(sticky.getId()))
                        .message(sticky.getMessage())
                        .actions(sticky.getActions().stream().map(a -> ActionStickyResp.builder()
                                .id(UUID.fromString(a.getId()))
                                .message(a.getMessage())
                                .build()).collect(toList()))
                        .locationId(UUID.fromString(sticky.getLocationId()))
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
                .ticketId(UUID.fromString(t.getId()))
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

    private void addLocation(RoutingContext rc) {
        final var locationReq = rc.getBodyAsJson().mapTo(AddLocationReq.class);
        Future.<Location>future(c -> {
            final var createLocation = CreateLocation.builder()
                    .parentLocation(locationReq.getParentLocation().toString())
                    .location(locationReq.getMessage())
                    .build();
            cartchufiService.createLocation(createLocation, c);
        }).map(this::locationSerializer)
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
                                    .roleId(asr.getRoleId().toString())
                                    .build())
                            .collect(toList()))
                    .locations(stickyReq.getLocations().stream()
                            .map(lsr -> CreateLocation.builder()
                                    .location(lsr.getLocation())
                                    .parentLocation(lsr.getParentLocation().toString())
                                    .build())
                            .collect(toList())
                    )
                    .build();
            cartchufiService.createSticky(createSticky, c);
        }).map(sticky -> StickyResp.builder()
                .id(UUID.fromString(sticky.getId()))
                .message(sticky.getMessage())
                .actions(sticky.getActions().stream().map(a -> ActionStickyResp.builder()
                        .id(UUID.fromString(a.getId()))
                        .message(a.getMessage())
                        .build()).collect(toList()))
                .locations(sticky.getLocations().stream().map(this::locationSerializer).collect(toList()))
                .build())
                .onSuccess(res -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(res)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(400).end(t.getMessage());
                });
    }

    private LocationResp locationSerializer(Location location) {
        return LocationResp.builder()
                .id(UUID.fromString(location.getId()))
                .parentLocation(UUID.fromString(location.getParentLocation()))
                .message(location.getText())
                .build();
    }

    private RoleResp roleSerializer(Role role) {
        return RoleResp.builder()
                .id(UUID.fromString(role.getId()))
                .role(role.getType())
                .build();
    }


}
