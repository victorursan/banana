package com.victor.banana.verticles;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class SupervisorVerticleTest {

    void supervisor_verticle_started_all_verticles() throws Throwable {
        final var testContext = new VertxTestContext();
        final var vertx = Vertx.vertx();
        vertx.deployVerticle(SupervisorVerticle::new, new DeploymentOptions(), testContext.completing());



    }
}