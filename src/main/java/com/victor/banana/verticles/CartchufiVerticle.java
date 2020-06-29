package com.victor.banana.verticles;

import com.victor.banana.services.*;
import com.victor.banana.services.impl.BookingServiceImpl;
import com.victor.banana.services.impl.PersonnelServiceImpl;
import com.victor.banana.services.impl.TicketingServiceImpl;
import com.victor.banana.utils.CallbackUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;

import static com.victor.banana.utils.Constants.EventbusAddress.*;
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

            final var personnelService = new PersonnelServiceImpl(dbService, keycloakClientService);
            final var personnelServiceBinder = new ServiceBinder(vertx)
                    .setAddress(PERSONNEL)
                    .registerLocal(PersonnelService.class, personnelService);

            final var ticketingService = new TicketingServiceImpl(telegramBotService, dbService);
            final var ticketingServiceBinder = new ServiceBinder(vertx)
                    .setAddress(TICKETING)
                    .registerLocal(TicketingService.class, ticketingService);

            final var bookingService = new BookingServiceImpl(dbService);
            final var bookingServiceBinder = new ServiceBinder(vertx)
                    .setAddress(BOOKING)
                    .registerLocal(BookingService.class, bookingService);

            return CallbackUtils.mergeFutures(
                    future(personnelServiceBinder::completionHandler),
                    future(bookingServiceBinder::completionHandler),
                    future(ticketingServiceBinder::completionHandler))
                    .mapEmpty();
        } catch (Exception e) {
            return failedFuture(e);
        }
    }
}
