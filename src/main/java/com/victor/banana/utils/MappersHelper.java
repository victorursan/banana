package com.victor.banana.utils;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.victor.banana.utils.ExceptionUtils.failedNoElementFound;
import static com.victor.banana.utils.ExceptionUtils.failedToMapElement;
import static java.util.stream.Collectors.toList;

public final class MappersHelper {
    private static final Logger log = LoggerFactory.getLogger(MappersHelper.class);

    public static <E, T> Function<E, Future<T>> mapperToFuture(Function<E, T> mapper) {
        return flatMapper(mapper.andThen(Future::succeededFuture));
    }

    public static <E, T> Function<E, Future<T>> flatMapper(Function<E, Future<T>> mapper) {
        return r -> {
            if (r != null) {
                try {
                    return mapper.apply(r);
                } catch (Exception e) {
                    log.error("Failed to map element", e);
                    return failedToMapElement(e.getMessage());
                }
            }
            log.error("No element found");
            return failedNoElementFound();
        };
    }

    public static <E, T> Function<E, Stream<T>> mapperToStream(Function<E, T> mapper) {
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

    public static <E, T> Function<List<E>, Future<List<T>>> mapTsF(Function<E, T> mapper) {
        return elements -> CallbackUtils.mergeFutures(elements.stream()
                .map(mapperToFuture(mapper))
                .collect(toList()));
    }

    public static <E, T> Function<List<E>, Future<List<T>>> flatMapTsF(Function<E, Future<T>> mapper) {
        return elements -> CallbackUtils.mergeFutures(elements.stream()
                .map(flatMapper(mapper))
                .collect(toList()));
    }

    public static <E, T> Function<List<E>, Future<List<T>>> mapTsF(Function<E, T> mapper, Comparator<? super T> comparator) {
        return elements -> CallbackUtils.mergeFutures(elements.stream()
                .map(mapperToFuture(mapper))
                .collect(toList()))
                .map(e -> {
                    e.sort(comparator); //todo
                    return e;
                });
    }

    public static <E, T> Function<List<E>, List<T>> mapTs(Function<E, T> mapper) {
        return elements -> elements.stream()
                .flatMap(mapperToStream(mapper))
                .collect(toList());
    }

    public static <E, T> Function<List<E>, List<T>> mapTs(Function<E, T> mapper, Comparator<? super T> comparator) {
        return elements -> elements.stream()
                .flatMap(mapperToStream(mapper))
                .sorted(comparator)
                .collect(toList());
    }


}
