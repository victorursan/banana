package com.victor.banana.verticles;

import com.victor.banana.services.TelegramBotService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;


public class HttpServerVerticle extends AbstractVerticle {

    private HttpServer server;
    private TelegramBotService telegramBotService;

    @Override
    public void start(Promise<Void> startPromise) {
        telegramBotService = TelegramBotService.createProxy(vertx, "telegram.bot.cartchufi");
        server = vertx.createHttpServer(new HttpServerOptions(vertx.getOrCreateContext().config()))
                .requestHandler(routes())
                .listen(l -> {
                    if (l.succeeded()) {
                        startPromise.complete();
                    } else {
                        startPromise.fail(l.cause());
                    }
                });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        server.close(stopPromise);
    }

    private Router routes() {
        final var router = Router.router(vertx);
        router.get("/healtz").handler(healthCheck());
        router.post("/api/message").handler(BodyHandler.create()).handler(message());
        return router;
    }

    private HealthCheckHandler healthCheck() {
        return HealthCheckHandler.create(vertx)
                .register("httpServer", f -> f.complete(Status.OK()));
    }

    private Handler<RoutingContext> message() {
        return rc -> {
            telegramBotService.sendMessage(rc.getBodyAsString());
            rc.response().setStatusCode(201).end();
        };
    }

}
