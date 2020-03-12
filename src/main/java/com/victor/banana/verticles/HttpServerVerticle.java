package com.victor.banana.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;


public class HttpServerVerticle extends AbstractVerticle {

    private HttpServer server;

    @Override
    public void start(Promise<Void> startPromise) {
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
        router.get("/healtz").handler(this::healtz);
        return router;
    }

    private void healtz(RoutingContext rc) {
        rc.response().setStatusCode(200).end("ok");
    }
}
