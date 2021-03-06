package com.victor.banana.controllers.db;

import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.sqlclient.Row;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;
import org.jooq.Record;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.victor.banana.utils.ExceptionUtils.failedDbExecution;
import static com.victor.banana.utils.MappersHelper.mapTsF;
import static com.victor.banana.utils.MappersHelper.mapperToFuture;
import static io.vertx.core.Future.succeededFuture;

public final class QueryHandler<T extends ReactiveClassicGenericQueryExecutor> {
    @NotNull
    private static final Logger log = LoggerFactory.getLogger(QueryHandler.class);
    @NotNull
    private final T queryExecutor;

    public QueryHandler(@NotNull T queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    @NotNull
    private static Handler<Throwable> failureHandler() {
        return t -> {
            if (!(t instanceof ServiceException)) {
                log.error("Unexpected error type", t);
            }
        };
    }

    @NotNull
    public static <R> Function<ReactiveClassicGenericQueryExecutor, Future<R>> withTransaction(Function<ReactiveClassicGenericQueryExecutor, Future<R>> transaction) {
        return q -> q.beginTransaction()
                .flatMap((t) -> transaction.apply(t)
                        .flatMap(r -> t.commit()
                                .onFailure(e -> log.error("Failed to commit transaction", e.getCause()))
                                .map(r))
                        .onFailure(e -> t.rollback()
                                .onFailure(er -> log.error("Failed to rollback transaction", er))
                        )
                );
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> execute(Function<DSLContext, ? extends Query> queryFunction, int expectedCount, String executedAction) {
        return execute(queryFunction, i -> i == expectedCount, executedAction);
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> execute(Function<DSLContext, ? extends Query> queryFunction, Predicate<Integer> condition, String executedAction) {
        return t -> t.execute(queryFunction)
                .flatMap(checkExecution(condition, executedAction));
    }

    @NotNull
    public static <Q extends Record, R> Function<ReactiveClassicGenericQueryExecutor, Future<R>> findOne(Function<DSLContext, ? extends ResultQuery<Q>> queryFunction, Function<Row, R> mapper) {
        return t -> t.findOneRow(queryFunction)
                .flatMap(mapperToFuture(mapper));
    }

    @NotNull
    public static <Q extends Record, R> Function<ReactiveClassicGenericQueryExecutor, Future<List<R>>> findMany(Function<DSLContext, ? extends ResultQuery<Q>> queryFunction, Function<Row, R> mapper) {
        return t -> t.findManyRow(queryFunction)
                .flatMap(mapTsF(mapper));
    }

    @NotNull
    public static <R extends Record> Function<DSLContext, ResultQuery<R>> selectWhere(
            Function<DSLContext, SelectJoinStep<R>> select,
            Condition condition) {
        return select.andThen(whereCondition(condition));
    }

    @NotNull
    private static <R extends Record> Function<SelectJoinStep<R>, SelectConditionStep<R>> whereCondition(Condition condition) {
        return s -> s.where(condition);
    }

    @NotNull
    private static Function<Integer, Future<Void>> checkExecution(Predicate<Integer> condition, String executedAction) {
        return i -> {
            if (condition.test(i)) {
                return succeededFuture();
            }
            return failedDbExecution(executedAction);
        };
    }

    @NotNull
    public final <E> Future<E> run(Function<T, Future<E>> query) {
        return query.apply(queryExecutor)
                .onFailure(failureHandler());
    }


}
