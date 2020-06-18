package com.victor.banana.verticles;

import com.victor.banana.services.CartchufiService;
import com.victor.banana.services.DatabaseService;
import com.victor.banana.services.KeycloakClientService;
import com.victor.banana.services.TelegramBotService;
import com.victor.banana.services.impl.CartchufiServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;

import static com.victor.banana.utils.Constants.EventbusAddress.CARTCHUFI_ENGINE;
import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.future;

public class CartchufiVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
        deployServiceBinder().onComplete(startPromise);
    }

    private Future<Void> deployServiceBinder() {
        try {
            final var dbService = DatabaseService.createProxy(vertx);
            final var telegramBotService = TelegramBotService.createProxy(vertx);
            final var keycloakClientService = KeycloakClientService.createProxy(vertx);
            final var service = new CartchufiServiceImpl(telegramBotService, dbService, keycloakClientService);

            return future(new ServiceBinder(vertx)
                    .setAddress(CARTCHUFI_ENGINE)
                    .register(CartchufiService.class, service)
                    ::completionHandler);
        } catch (Exception e) {
            return failedFuture(e);
        }
    }
}
