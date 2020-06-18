package com.victor.banana.utils;

import io.vertx.core.Future;
import io.vertx.serviceproxy.ServiceException;
import org.jetbrains.annotations.NotNull;

import static io.vertx.core.Future.failedFuture;

public final class ExceptionUtils {
    public static final int FAILED_DB_EXECUTION = 1;
    public static final int FAILED_TO_MAP_ELEMENT = 2;
    public static final int FAILED_NO_ELEMENT_FOUND = 3;


    @NotNull
    private static <T> Future<T> serviceException(int failureCode, String message) {
        return failedFuture(new ServiceException(failureCode, message));
    }

    @NotNull
    public static <T> Future<T> failedDbExecution(String executedAction) {
        return serviceException(FAILED_DB_EXECUTION, String.format("Failed [db] execution to: %s", executedAction));
    }

    @NotNull
    public static <T> Future<T> failedToMapElement(String executedAction) {
        return serviceException(FAILED_TO_MAP_ELEMENT, String.format("Failed element mapping: %s", executedAction));
    }

    @NotNull
    public static <T> Future<T> failedNoElementFound() {
        return serviceException(FAILED_NO_ELEMENT_FOUND, "Failed no element found");
    }
}
