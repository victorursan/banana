package com.victor.banana.verticles;

import com.victor.banana.models.events.CreateSticky;
import com.victor.banana.models.events.Sticky;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.models.requests.ActionSelectedReq;
import com.victor.banana.models.requests.StickyReq;
import com.victor.banana.models.responses.ActionStickyResp;
import com.victor.banana.models.responses.StickyResp;
import com.victor.banana.models.responses.TicketRes;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.victor.banana.utils.Constants.EventbusAddress.CARTCHUFI_ENGINE;
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
        router.get("/healtz").handler(healthCheck());
        router.get("/api/tickets/:ticketId").handler(this::getTicket);
        router.post("/api/stickies").handler(BodyHandler.create()).handler(this::addSticky);
        router.get("/api/stickies/:stickyId").handler(this::scanSticky);
        router.post("/api/actions").handler(BodyHandler.create()).handler(this::actionSelected);
        return router;
    }

    private HealthCheckHandler healthCheck() {
        return HealthCheckHandler.create(vertx)
                .register("httpServer", f -> f.complete(Status.OK()));
    }

    private void scanSticky(RoutingContext rc) {
        final var stickyId = rc.request().getParam("stickyId");
        Future.<Sticky>future(c -> cartchufiService.getSticky(stickyId, c))
                .map(sticky -> StickyResp.builder()
                        .id(sticky.getId())
                        .message(sticky.getMessage())
                        .actions(sticky.getActions().stream().map(a -> ActionStickyResp.builder()
                                .id(a.getId())
                                .message(a.getMessage())
                                .build()).collect(toList()))
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
        final var actionSelected = rc.getBodyAsJson().mapTo(ActionSelectedReq.class);
        Future.<Ticket>future(t -> cartchufiService.actionSelected(actionSelected.getActionId(), t))
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
                    .build();
            cartchufiService.createSticky(createSticky, c);
        }).map(sticky -> StickyResp.builder()
                .id(sticky.getId())
                .message(sticky.getMessage())
                .actions(sticky.getActions().stream().map(a -> ActionStickyResp.builder()
                        .id(a.getId())
                        .message(a.getMessage())
                        .build()).collect(toList()))
                .build())
                .onSuccess(res -> rc.response().setStatusCode(201).end(Json.encodeToBuffer(res)))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(400).end(t.getMessage());
                });
    }


}
