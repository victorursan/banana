package com.victor.banana.verticles;

import com.victor.banana.services.BookingService;
import com.victor.banana.services.PersonnelService;
import com.victor.banana.services.TicketingService;
import com.victor.banana.services.impl.BookingAPIService;
import com.victor.banana.services.impl.TicketingAPIService;
import com.victor.banana.utils.CallbackUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static io.vertx.core.http.HttpHeaders.*;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.*;
import static io.vertx.ext.web.handler.LoggerFormat.SHORT;


public class HttpServerVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(HttpServerVerticle.class);
    private HttpServer server;

    @Override
    public void start(Promise<Void> startPromise) {
        final var config = vertx.getOrCreateContext().config();
        final var httpConfig = config.getJsonObject("http");
        final var keyCloak = config.getJsonObject("keycloak");
        final var allowedOrigin = config.getString("allowedOrigin");
        final var selfHost = config.getString("selfHost");

        final var personnelService = PersonnelService.createProxy(vertx);
        final var ticketingService = TicketingService.createProxy(vertx);
        final var bookingService = BookingService.createProxy(vertx);

        final var ticketingApi = new TicketingAPIService(ticketingService, personnelService);
        final var bookingApi = new BookingAPIService(bookingService, personnelService);

        final var allowedHeaders = Set.of(
                ACCESS_CONTROL_ALLOW_ORIGIN.toString(),
                ACCESS_CONTROL_ALLOW_HEADERS.toString(),
                ACCESS_CONTROL_ALLOW_METHODS.toString(),
                ACCESS_CONTROL_ALLOW_CREDENTIALS.toString(),
                AUTHORIZATION.toString(),
                CONTENT_TYPE.toString());
        final var allowedMethods = Set.of(GET, POST, PUT, DELETE);

        final var clientOptions = new OAuth2ClientOptions(keyCloak);

        final var bookingApiYaml = Future.<OpenAPI3RouterFactory>future(f -> OpenAPI3RouterFactory.create(vertx, "bookingAPI.yaml", f));
        final var ticketingApiYaml = Future.<OpenAPI3RouterFactory>future(f -> OpenAPI3RouterFactory.create(vertx, "ticketingAPI.yaml", f));
        final var bookingRouterF = bookingApi.routes(bookingApiYaml);
        final var ticketingRouterF = ticketingApi.routes(ticketingApiYaml);

        CallbackUtils.mergeFutures(List.of(bookingRouterF, ticketingRouterF)).flatMap(routers -> {
            final var router = Router.router(vertx);

            router.route().handler(LoggerHandler.create(SHORT));
            router.errorHandler(400, rc -> log.error(rc.getBodyAsString(), rc.failure()));
            router.errorHandler(404, rc -> log.error(rc.getBodyAsString(), rc.failure()));
            router.errorHandler(405, rc -> log.error(rc.getBodyAsString(), rc.failure()));
            router.errorHandler(406, rc -> log.error(rc.getBodyAsString(), rc.failure()));
            router.errorHandler(415, rc -> log.error(rc.getBodyAsString(), rc.failure()));
            router.route().handler(CorsHandler.create(allowedOrigin)
                    .allowedHeaders(allowedHeaders)
                    .allowCredentials(true)
                    .allowedMethods(allowedMethods));

            return Future.<OAuth2Auth>future(h -> KeycloakAuth.discover(vertx, clientOptions, h))
                    .flatMap(keycloakHandler(selfHost, router))
                    .map(oauth2 -> {
                        router.route("/api/*").handler(oauth2); //todo
                        router.get("/healthz").handler(healthCheck());
                        routers.forEach(subRouter -> router.mountSubRouter("/api", subRouter));
                        return oauth2;
                    })
                    .<HttpServer>flatMap(ignore ->
                            Future.future(vertx.createHttpServer(new HttpServerOptions(httpConfig))
                                    .requestHandler(router)
                                    ::listen));
        }).onSuccess(httpServer -> server = httpServer)
                .<Void>mapEmpty()
                .onComplete(startPromise);
//        bookingApi.routes(bookingApiYaml);
//        ticketingApi.routes(ticketingApiYaml)
//                .flatMap(subrouter -> {
//                    final var router = Router.router(vertx);
//
//                    router.route().handler(LoggerHandler.create(SHORT));
//                    router.errorHandler(400, rc -> log.error(rc.getBodyAsString(), rc.failure()));
//                    router.errorHandler(404, rc -> log.error(rc.getBodyAsString(), rc.failure()));
//                    router.errorHandler(405, rc -> log.error(rc.getBodyAsString(), rc.failure()));
//                    router.errorHandler(406, rc -> log.error(rc.getBodyAsString(), rc.failure()));
//                    router.errorHandler(415, rc -> log.error(rc.getBodyAsString(), rc.failure()));
//                    router.route().handler(CorsHandler.create(allowedOrigin)
//                            .allowedHeaders(allowedHeaders)
//                            .allowCredentials(true)
//                            .allowedMethods(allowedMethods));
//
//                    return Future.<OAuth2Auth>future(h -> KeycloakAuth.discover(vertx, clientOptions, h))
//                            .flatMap(keycloakHandler(selfHost, router))
//                            .map(oauth2 -> {
//                                router.route("/api/*").handler(oauth2); //todo
////                                router.get("/logout").handler(this::handleLogout);
//                                router.get("/healthz").handler(healthCheck());
//
//                                router.mountSubRouter("/api", subrouter);
//                                return oauth2;
//                            })
//                            .<HttpServer>flatMap(ignore ->
//                                    Future.future(vertx.createHttpServer(new HttpServerOptions(httpConfig))
//                                    .requestHandler(router)
//                                    ::listen));
//                })
//                .onSuccess(httpServer -> server = httpServer)
//                .<Void>mapEmpty()
//                .onComplete(startPromise);
    }

    private Function<OAuth2Auth, Future<AuthHandler>> keycloakHandler(String selfHost, Router router) {
        return oauth2Auth -> {
            if (oauth2Auth == null) {
                return Future.failedFuture("Could not configure Keycloak integration via OpenID Connect Discovery Endpoint. Is Keycloak running?");
            }
            final var callbackPath = "/callback";
            final var authHandler = OAuth2AuthHandler.create(oauth2Auth, URI.create(selfHost).resolve(callbackPath).toString())
                    .setupCallback(router.get(callbackPath))
                    // Additional scopes: openid for OpenID Connect
                    .addAuthority("openid");
            return Future.succeededFuture(authHandler);
        };
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        Optional.ofNullable(server)
                .ifPresent(s -> s.close(stopPromise));
    }

    private HealthCheckHandler healthCheck() {
        return HealthCheckHandler.create(vertx)
                .register("httpServer", f -> f.complete(Status.OK()));
    }

//    private void handleLogout(RoutingContext ctx) {
//        final var oAuth2Token = (OAuth2TokenImpl) ctx.user();
//        oAuth2Token.logout(res -> {
//            if (!res.succeeded()) {
//                // the user might not have been logged out, to know why:
//                log.error("failed to logout user", res.cause());
//                ctx.response().setStatusCode(500).end("Logout failed");
//                return;
//            }
//            oAuth2Token.clearCache();
//            ctx.response().putHeader("location", "/?logout=true").setStatusCode(302).end();
//        });
//    }


}
