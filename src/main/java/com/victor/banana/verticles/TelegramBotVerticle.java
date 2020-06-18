package com.victor.banana.verticles;

import com.victor.banana.models.configs.TelegramBotConfig;
import com.victor.banana.services.CartchufiService;
import com.victor.banana.services.TelegramBotService;
import com.victor.banana.services.impl.TelegramBotServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;

import static com.victor.banana.utils.Constants.EventbusAddress.TELEGRAM_BOT;
import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.future;

public class TelegramBotVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
        final var configs = vertx.getOrCreateContext().config();
        deployServiceBinder(configs)
                .onComplete(startPromise);
    }

    private Future<Void> deployServiceBinder(JsonObject config) {
        try {
            final var cartchufiService = CartchufiService.createProxy(vertx);
            final var telegramConf = config.mapTo(TelegramBotConfig.class);
            final var service = new TelegramBotServiceImpl(telegramConf, cartchufiService);

            return future(new ServiceBinder(vertx)
                    .setAddress(TELEGRAM_BOT)
                    .register(TelegramBotService.class, service)
                    ::completionHandler);
        } catch (Exception e) {
            return failedFuture(e);
        }

    }


}
