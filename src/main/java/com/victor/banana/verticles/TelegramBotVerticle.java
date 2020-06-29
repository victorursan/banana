package com.victor.banana.verticles;

import com.victor.banana.models.configs.TelegramBotConfig;
import com.victor.banana.services.PersonnelService;
import com.victor.banana.services.TicketingService;
import com.victor.banana.services.TelegramBotService;
import com.victor.banana.services.impl.TelegramBotServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;

import static com.victor.banana.utils.Constants.EventbusAddress.TELEGRAM_BOT;
import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.future;

public class TelegramBotVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
        deployServiceBinder().onComplete(startPromise);
    }

    private Future<Void> deployServiceBinder() {
        try {
            final var config = vertx.getOrCreateContext().config();
            final var cartchufiService = TicketingService.createProxy(vertx);
            final var personnelService = PersonnelService.createProxy(vertx);
            final var telegramConf = config.mapTo(TelegramBotConfig.class);

            final var service = new TelegramBotServiceImpl(telegramConf, cartchufiService, personnelService);
            final var serviceBinder = new ServiceBinder(vertx)
                    .setAddress(TELEGRAM_BOT)
                    .registerLocal(TelegramBotService.class, service);


            return future(serviceBinder::completionHandler);
        } catch (Exception e) {
            return failedFuture(e);
        }

    }


}
