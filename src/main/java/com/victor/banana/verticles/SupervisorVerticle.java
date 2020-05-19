package com.victor.banana.verticles;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;

import java.util.Optional;
import java.util.function.Supplier;

import static io.vertx.core.Future.future;

public class SupervisorVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
        final var configFile = Optional.ofNullable(System.getenv("CONFIG_FILE")).orElse("config.json");
        final var retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
                .addStore(new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", configFile))));
        future(retriever::getConfig)
                .flatMap(this::deployVerticles)
                .onComplete(startPromise);
    }

    private Future<Void> deployVerticles(JsonObject configs) {
        final var httpConf = configs.getJsonObject("httpServer");
        final var botConf = configs.getJsonObject("bot");
        final var dbConf = configs.getJsonObject("db");
        DatabindCodec.mapper().registerModule(new Jdk8Module());
        return deployVerticle(HttpServerVerticle::new, httpConf)
                .flatMap(ignore -> deployVerticle(TelegramBotVerticle::new, botConf))
                .flatMap(ignore -> deployVerticle(DatabaseVerticle::new, dbConf))
                .flatMap(ignore -> deployVerticle(CartchufiVerticle::new, null));
    }

    private Future<Void> deployVerticle(Supplier<Verticle> verticleSupply, JsonObject config) {
        return future((Promise<String> handler) ->
                vertx.deployVerticle(verticleSupply, new DeploymentOptions().setConfig(config), handler))
                .mapEmpty();
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        stopPromise.complete();
    }
}
