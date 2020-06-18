package com.victor.banana.utils;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.updateshandlers.SentCallback;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class CallbackUtils {
    private static final Logger log = LoggerFactory.getLogger(CallbackUtils.class);

    public static <T extends Serializable> SentCallback<T> sentCallback(Promise<T> messagePromise) {
        return new SentCallback<>() {
            @Override
            public void onResult(BotApiMethod<T> method, T response) {
                messagePromise.complete(response);
            }

            @Override
            public void onError(BotApiMethod<T> method, TelegramApiRequestException apiException) {
                messagePromise.fail(apiException);
            }

            @Override
            public void onException(BotApiMethod<T> method, Exception exception) {
                messagePromise.fail(exception);
            }
        };
    }

    public static Future<List<?>> mergeFutures(Future<?>... futures) {
        if (futures.length == 0) {
            log.debug("Received empty list of futures to merge.");
            return Future.succeededFuture(List.of());
        }
        return CompositeFuture.join(Arrays.asList(futures))
                .flatMap(r -> {
                    if (r.succeeded()) {
                        return Future.succeededFuture(Arrays.stream(futures)
                                .map(Future::result)
                                .collect(Collectors.toList()));
                    }
                    return Future.failedFuture(r.cause());
                });
    }

    @SuppressWarnings("all")
    public static <T> Future<List<T>> mergeFutures(List<Future<T>> futures) {
//        final var result = Promise.<List<T>>promise();
        if (futures.isEmpty()) {
            log.debug("Received empty list of futures to merge.");
            return Future.succeededFuture(List.of());
        }
        return CompositeFuture.join((List) futures)
                .flatMap(r -> {
                    if (r.succeeded()) {
                        return Future.succeededFuture(futures.stream()
                                .map(Future::result)
                                .collect(Collectors.toList()));
                    }
                    return Future.failedFuture(r.cause());
                });
//                .map(f -> futures.stream()
//                        .map(Future::result)
//                        .collect(Collectors.toList()));
//                .onComplete(result);
//        return result.future();

    }

}
