package com.victor.banana.verticles;

import com.victor.banana.services.impl.CartchufiBotService;
import com.victor.banana.services.TelegramBotService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;

public class TelegramBotVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
        final var service = new CartchufiBotService();

        new ServiceBinder(vertx)
                .setAddress("telegram.bot.cartchufi")
                .register(TelegramBotService.class, service);
        startPromise.complete();
    }

}
