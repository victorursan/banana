package com.victor.banana.services;

import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;

public interface APIService {
    Future<Router> routes(Future<OpenAPI3RouterFactory> routerFactoryFuture);
}
