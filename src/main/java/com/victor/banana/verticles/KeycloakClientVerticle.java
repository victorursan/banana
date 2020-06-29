package com.victor.banana.verticles;

import com.victor.banana.models.configs.KeycloakClientConfig;
import com.victor.banana.services.DatabaseService;
import com.victor.banana.services.KeycloakClientService;
import com.victor.banana.services.impl.DatabaseServiceImpl;
import com.victor.banana.services.impl.KeycloakClientServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;

import static com.victor.banana.utils.Constants.EventbusAddress.DATABASE;
import static com.victor.banana.utils.Constants.EventbusAddress.KEYCLOAK_CLIENT;
import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.future;

public class KeycloakClientVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(KeycloakClientVerticle.class);
    public KeycloakClientServiceImpl service;

    @Override
    public void start(Promise<Void> startPromise) {
        deployServiceBinder().onComplete(startPromise);
    }

    private Future<Void> deployServiceBinder() {
        try {
            final var configs = vertx.getOrCreateContext().config();
            final var conf = configs.mapTo(KeycloakClientConfig.class);

            final var workerExec = vertx.createSharedWorkerExecutor("keycloak-client");
            service = new KeycloakClientServiceImpl(conf, workerExec);

            final var serviceBinder = new ServiceBinder(vertx)
                    .setAddress(KEYCLOAK_CLIENT)
                    .registerLocal(KeycloakClientService.class, service);
            return future(serviceBinder::completionHandler);
        } catch (Exception e) {
            return failedFuture(e);
        }
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        try {
            if (service != null) {
                service.close();
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
