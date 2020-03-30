package com.victor.banana.utils;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.updateshandlers.SentCallback;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public final class CallbackUtils {

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

    public static <T> Future<List<T>> mergeFutures(List<Future<T>> futures) {
        final var result = Promise.<List<T>>promise();
        CompositeFuture.join((List) futures)
                .map(futures.stream()
                        .map(Future::result)
                        .collect(Collectors.toList()))
                .setHandler(result);
        return result.future();

    }

}
