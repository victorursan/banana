package com.victor.banana;


import com.victor.banana.verticles.SupervisorVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class Main {

    public static void main(String[] args) {
        final var vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(40));
        vertx.deployVerticle(SupervisorVerticle::new, new DeploymentOptions());
    }
}
