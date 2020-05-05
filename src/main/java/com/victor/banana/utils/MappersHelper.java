package com.victor.banana.utils;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.util.stream.Collectors.toList;

public final class MappersHelper {
    private static final Logger log = LoggerFactory.getLogger(MappersHelper.class);

    public static <E, T> Function<E, Future<T>> fToTF(Function<E, T> mapper) {
        return r -> {
            if (r != null) {
                try {
                    final var t = mapper.apply(r);
                    return succeededFuture(t);
                } catch (Exception e) {
                    log.error("Failed to map element", e);
                    return failedFuture(e);
                }
            }
            log.error("No element found");
            return failedFuture("No Row found");
        };
    }

    public static <E, T> Function<E, Stream<T>> fToTS(Function<E, T> mapper) {
        return r -> {
            if (r != null) {
                try {
                    final var t = mapper.apply(r);
                    return Stream.of(t);
                } catch (Exception e) {
                    log.error("Failed to map element", e);
                    return Stream.empty();
                }
            }
            log.error("No element found");
            return Stream.empty();
        };
    }

    public static <E, T> Function<List<E>, List<T>> mapTs(Function<E, T> mapper) {
        return elements -> elements.stream()
                .flatMap(fToTS(mapper))
                .collect(toList());
    }


}
