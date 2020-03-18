package com.victor.banana.services;


import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Vertx;

@ProxyGen
@VertxGen
public interface TelegramBotService {

    void sendMessage(String customerName);

    static TelegramBotService createProxy(Vertx vertx, String address) {
        return new TelegramBotServiceVertxEBProxy(vertx, address);
    }
}
