package com.victor.banana.verticles;

import com.victor.banana.services.CartchufiService;
import com.victor.banana.services.impl.APIServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.impl.OAuth2TokenImpl;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static com.victor.banana.utils.Constants.EventbusAddress.CARTCHUFI_ENGINE;
import static io.vertx.core.http.HttpHeaders.*;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.*;


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

        final var cartchufiService = CartchufiService.createProxy(vertx, CARTCHUFI_ENGINE);
        final var hs = new APIServiceImpl(cartchufiService);

        final var allowedHeaders = Set.of(
                ACCESS_CONTROL_ALLOW_ORIGIN.toString(),
                ACCESS_CONTROL_ALLOW_HEADERS.toString(),
                ACCESS_CONTROL_ALLOW_METHODS.toString(),
                ACCESS_CONTROL_ALLOW_CREDENTIALS.toString(),
                AUTHORIZATION.toString(),
                CONTENT_TYPE.toString());
        final var allowedMethods = Set.of(GET, POST, PUT, DELETE);


        final var clientOptions = new OAuth2ClientOptions(keyCloak);

        hs.routes(Future.future(f -> OpenAPI3RouterFactory.create(vertx, "cartchufi.yaml", f)))
                .flatMap(subrouter -> {
                    final var sessionStore = LocalSessionStore.create(vertx);
                    final var sessionHandler = SessionHandler.create(sessionStore)
                            .setCookieHttpOnlyFlag(true);
//                            .setCookieSecureFlag(true); todo https

                    final var router = Router.router(vertx);

                    router.route().handler(LoggerHandler.create());

                    router.route().handler(CorsHandler.create(allowedOrigin)
                            .allowedHeaders(allowedHeaders)
                            .allowCredentials(true)
                            .allowedMethods(allowedMethods));

                    return Future.<OAuth2Auth>future(h -> KeycloakAuth.discover(vertx, clientOptions, h))
                            .flatMap(keycloakHandler(selfHost, sessionHandler, router))
                            .map(oauth2 -> {
                                router.route("/api/*").handler(oauth2); //todo
                                router.get("/logout").handler(this::handleLogout);
                                router.get("/healthz").handler(healthCheck());
                                router.mountSubRouter("/api", subrouter);
                                return oauth2;
                            })
                            .<HttpServer>flatMap(ignore ->
                                    Future.future(vertx.createHttpServer(new HttpServerOptions(httpConfig))
                                    .requestHandler(router)
                                    ::listen));
                })
                .onSuccess(httpServer -> server = httpServer)
                .<Void>mapEmpty()
                .onComplete(startPromise);
    }

    private Function<OAuth2Auth, Future<AuthHandler>> keycloakHandler(String selfHost, SessionHandler sessionHandler, Router router) {
        return oauth2Auth -> {
            if (oauth2Auth == null) {
                return Future.failedFuture("Could not configure Keycloak integration via OpenID Connect Discovery Endpoint. Is Keycloak running?");
            }
            sessionHandler.setAuthProvider(oauth2Auth);
            router.route().handler(sessionHandler);

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


    private void handleLogout(RoutingContext ctx) {
        final var oAuth2Token = (OAuth2TokenImpl) ctx.user();
        oAuth2Token.logout(res -> {
            if (!res.succeeded()) {
                // the user might not have been logged out, to know why:
                log.error("failed to logout user", res.cause());
                ctx.response().setStatusCode(500).end("Logout failed");
                return;
            }
            oAuth2Token.clearCache();
            ctx.session().destroy();
            ctx.response().putHeader("location", "/?logout=true").setStatusCode(302).end();
        });
    }


}
