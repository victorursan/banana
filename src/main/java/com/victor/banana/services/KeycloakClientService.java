package com.victor.banana.services;

import com.victor.banana.models.events.KeyUserDelete;
import com.victor.banana.models.events.KeyUserRoleUpdate;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import static com.victor.banana.utils.Constants.DeliveryOptionsConstants.LOCAL_DELIVERY;
import static com.victor.banana.utils.Constants.EventbusAddress.KEYCLOAK_CLIENT;

@ProxyGen
@VertxGen
public interface KeycloakClientService {

    static KeycloakClientService createProxy(Vertx vertx) {
        return new KeycloakClientServiceVertxEBProxy(vertx, KEYCLOAK_CLIENT, LOCAL_DELIVERY);
    }

    void deleteUser(KeyUserDelete userDelete, Handler<AsyncResult<Void>> result);

    void userRoleUpdate(KeyUserRoleUpdate roleUpdate, Handler<AsyncResult<Void>> result);

}
