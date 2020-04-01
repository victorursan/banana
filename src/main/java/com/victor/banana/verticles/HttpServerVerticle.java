package com.victor.banana.verticles;

import com.victor.banana.models.events.ActionSelected;
import com.victor.banana.models.events.stickies.CreateSticky;
import com.victor.banana.models.events.stickies.Sticky;
import com.victor.banana.models.events.stickies.StickyLocation;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.models.requests.ActionSelectedReq;
import com.victor.banana.models.requests.StickyReq;
import com.victor.banana.models.responses.*;
import com.victor.banana.services.CartchufiService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.victor.banana.utils.Constants.EventbusAddress.CARTCHUFI_ENGINE;
import static io.vertx.core.http.HttpHeaders.*;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static java.util.stream.Collectors.toList;


public class HttpServerVerticle extends AbstractVerticle {
    private static Logger log = LoggerFactory.getLogger(HttpServerVerticle.class);
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
        final var allowedHeaders = Set.of(ACCESS_CONTROL_ALLOW_ORIGIN.toString(),ORIGIN.toString(), CONTENT_TYPE.toString(), ACCEPT.toString());
        final var allowedMethods = Set.of(GET, POST);

        router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));

        router.get("/healtz").handler(healthCheck());
        router.get("/api/tickets/:ticketId").handler(this::getTicket);
        router.post("/api/stickies").handler(BodyHandler.create()).handler(this::addSticky);
        router.get("/api/stickies/:stickyLocationId").handler(this::scanSticky);
        router.post("/api/actions").handler(BodyHandler.create()).handler(this::actionSelected);
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
                        .id(sticky.getId())
                        .message(sticky.getMessage())
                        .actions(sticky.getActions().stream().map(a -> ActionStickyResp.builder()
                                .id(a.getId())
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

    private void actionSelected(RoutingContext rc) {
        final var actionSelectedReq = rc.getBodyAsJson().mapTo(ActionSelectedReq.class);
        final var actionSelected = ActionSelected.builder()
                .actionId(actionSelectedReq.getActionId())
                .locationId(actionSelectedReq.getLocationId())
                .build();
        Future.<Ticket>future(t -> cartchufiService.actionSelected(actionSelected, t))
                .map(this::ticketSerializer)
                .onSuccess(res -> rc.response().setStatusCode(200).end(Json.encodeToBuffer(res)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(500).end(t.getMessage());
                });
    }

    private TicketRes ticketSerializer(Ticket t) {
        return TicketRes.builder()
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

    private void addSticky(RoutingContext rc) {
        final var stickyReq = rc.getBodyAsJson().mapTo(StickyReq.class);
        Future.<Sticky>future(c -> {
            final var createSticky = CreateSticky.builder()
                    .message(stickyReq.getMessage())
                    .actions(stickyReq.getActions())
                    .locations(stickyReq.getLocations())
                    .build();
            cartchufiService.createSticky(createSticky, c);
        }).map(sticky -> StickyResp.builder()
                .id(sticky.getId())
                .message(sticky.getMessage())
                .actions(sticky.getActions().stream().map(a -> ActionStickyResp.builder()
                        .id(a.getId())
                        .message(a.getMessage())
                        .build()).collect(toList()))
                .locations(sticky.getLocations().stream().map(l -> LocationResp.builder()
                        .id(l.getId())
                        .message(l.getText())
                        .build()).collect(toList()))
                .build())
                .onSuccess(res -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(res)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(400).end(t.getMessage());
                });
    }


}
