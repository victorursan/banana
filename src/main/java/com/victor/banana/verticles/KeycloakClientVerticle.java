package com.victor.banana.verticles;

import com.victor.banana.models.configs.KeycloakClientConfig;
import com.victor.banana.services.KeycloakClientService;
import com.victor.banana.services.impl.KeycloakClientServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.serviceproxy.ServiceBinder;

import static com.victor.banana.utils.Constants.EventbusAddress.KEYCLOAK_CLIENT;

public class KeycloakClientVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(KeycloakClientVerticle.class);
    public KeycloakClientServiceImpl service;

    @Override
    public void start(Promise<Void> startPromise) {
        try {
            final var configs = vertx.getOrCreateContext().config();
            final var conf = configs.mapTo(KeycloakClientConfig.class);

            final var workerExec = vertx.createSharedWorkerExecutor("keycloak-client");
            service = new KeycloakClientServiceImpl(conf, workerExec);

            new ServiceBinder(vertx)
                    .setAddress(KEYCLOAK_CLIENT)
                    .register(KeycloakClientService.class, service)
                    .completionHandler(startPromise);
        } catch (Exception e) {
            startPromise.fail(e);
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
