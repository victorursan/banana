package com.victor.banana.verticles;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

public class SupervisorVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        final var retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
                .addStore(new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", "config.json"))));
        retriever.getConfig(json -> {
            if (json.succeeded()) {
                final var httpConf = json.result().getJsonObject("http");
                vertx.deployVerticle(HttpServerVerticle::new, new DeploymentOptions().setConfig(httpConf), t -> {
                    if (t.succeeded()) {
                        vertx.deployVerticle(TelegramBotVerticle::new, new DeploymentOptions().setConfig(httpConf), t2 -> {
                            if (t2.succeeded()) {
                                startPromise.complete();
                            } else {
                                startPromise.fail(t2.cause());
                            }
                        });
                    } else {
                        startPromise.fail(t.cause());
                    }
                });

            } else {
                json.cause().printStackTrace();
                startPromise.fail(json.cause());
            }
        });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        stopPromise.complete();
    }
}
