package com.victor.banana.verticles;

import com.victor.banana.services.DatabaseService;
import com.victor.banana.services.impl.DatabaseServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;

import static com.victor.banana.utils.Constants.EventbusAddress.DATABASE;
import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.future;

public class DatabaseVerticle extends AbstractVerticle {
    private PgPool pgPool;

    @Override
    public void start(Promise<Void> startPromise) {
        deployServiceBinder().onComplete(startPromise);
    }

    private Future<Void> deployServiceBinder() {
            try {
                final var configs = vertx.getOrCreateContext().config();
                final var connectionConf = configs.getJsonObject("connection");
                final var poolConf = configs.getJsonObject("pool");

                final var connectOptions = new PgConnectOptions(connectionConf);
                final var poolOptions = new PoolOptions(poolConf);

                pgPool = PgPool.pool(vertx, connectOptions, poolOptions);
                final var service = new DatabaseServiceImpl(pgPool);

                final var serviceBinder = new ServiceBinder(vertx)
                        .setAddress(DATABASE)
                        .registerLocal(DatabaseService.class, service);
                return future(serviceBinder::completionHandler);
        } catch (Exception e) {
            return failedFuture(e);
        }
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        try {
            if (pgPool != null) {
                pgPool.close();
            }
            stopPromise.complete();
        } catch (Exception e) {
            stopPromise.fail(e);
        }

    }
}
