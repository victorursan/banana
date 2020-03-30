package com.victor.banana.verticles;

import com.victor.banana.models.events.Action;
import com.victor.banana.models.events.Sticky;
import com.victor.banana.models.events.StickyAction;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.requests.StickyReq;
import com.victor.banana.models.responses.TicketRes;
import com.victor.banana.services.CartchufiService;
import com.victor.banana.services.DatabaseService;
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

import java.util.UUID;

import static com.victor.banana.utils.Constants.EventbusAddress.*;
import static java.util.stream.Collectors.toList;


public class HttpServerVerticle extends AbstractVerticle {
    private static Logger log = LoggerFactory.getLogger(HttpServerVerticle.class);
    private HttpServer server;
    private CartchufiService cartchufiService;
    private DatabaseService databaseService;

    @Override
    public void start(Promise<Void> startPromise) {
        cartchufiService = CartchufiService.createProxy(vertx, CARTCHUFI_ENGINE);
        databaseService = DatabaseService.createProxy(vertx, DATABASE);
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
//        router.post("/api/messages").handler(BodyHandler.create()).handler(this::sendMessage);
        router.post("/api/stickies").handler(BodyHandler.create()).handler(this::addSticky);
//        router.get("/api/stickies/:stickyId").handler(this::stickyScan); //todo
        router.get("/api/actions/:actionId").handler(this::stickyScan);
        return router;
    }

    private HealthCheckHandler healthCheck() {
        return HealthCheckHandler.create(vertx)
                .register("httpServer", f -> f.complete(Status.OK()));
    }

//    private void sendMessage(RoutingContext rc) {
//        telegramBotService.sendMessage(rc.getBodyAsString(), t -> rc.response().setStatusCode(201).end());
//    }

    private void stickyScan(RoutingContext rc) {
        final var actionId = rc.request().getParam("actionId");
        Future.<StickyAction>future(t -> databaseService.getStickyAction(actionId, t))
                .flatMap(st -> Future.<Ticket>future(t -> cartchufiService.stickyActionScanned(st, t)))
                .onSuccess(t -> rc.response().setStatusCode(200).end(Json.encode(new TicketRes(t.getId()))))
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(500).end(t.getMessage());
                });

    }

    private void addSticky(RoutingContext rc) {
        Future.<Boolean>future(c -> {
            final var stickyReq = rc.getBodyAsJson().mapTo(StickyReq.class);

            final var sticky = Sticky.builder()
                    .id(UUID.randomUUID().toString())
                    .message(stickyReq.getMessage())
                    .actions(stickyReq.getActions().stream().map(message ->
                            Action.builder().id(UUID.randomUUID().toString()).message(message).build()
                    ).collect(toList()))
                    .build();
            databaseService.addSticky(sticky, c);
        })
                .onSuccess(r -> {
                    if (r) {
                        rc.response().setStatusCode(201).end();
                    } else {
                        rc.response().setStatusCode(400).end("sticky could not be created");
                    }
                })
                .onFailure(t -> {
                    log.error(t.getMessage(), t);
                    rc.response().setStatusCode(500).end(t.getMessage());
                });
    }


}
