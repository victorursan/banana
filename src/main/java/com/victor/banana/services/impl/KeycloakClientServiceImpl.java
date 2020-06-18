package com.victor.banana.services.impl;

import com.victor.banana.models.configs.KeycloakClientConfig;
import com.victor.banana.models.events.KeyUserDelete;
import com.victor.banana.models.events.KeyUserRoleUpdate;
import com.victor.banana.services.KeycloakClientService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

public class KeycloakClientServiceImpl implements KeycloakClientService, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(KeycloakClientServiceImpl.class);
    private final Keycloak keycloak;
    private final UsersResource usersResource;
    private final WorkerExecutor executor;

    public KeycloakClientServiceImpl(KeycloakClientConfig conf, WorkerExecutor executor) {
        this.executor = executor;
        this.keycloak = Keycloak.getInstance(conf.getServerUrl(),
                conf.getRealm(),
                conf.getUsername(),
                conf.getPassword(),
                conf.getClientId(),
                conf.getClientSecret());
        final var realm = keycloak.realm(conf.getRealm());
        usersResource = realm.users();
    }

    @Override
    public final void deleteUser(KeyUserDelete userDelete, Handler<AsyncResult<Void>> result) {
        this.executor.executeBlocking(t -> {
            final var user = usersResource.delete(userDelete.getPersonnelId().toString());
            log.info(user.getStatusInfo()); //todo
            t.complete();
        }, result);
    }

    @Override
    public final void userRoleUpdate(KeyUserRoleUpdate roleUpdate, Handler<AsyncResult<Void>> result) {
        this.executor.executeBlocking(t -> {
            try {
                final var user = usersResource.get(roleUpdate.getPersonnelId().toString());
                final var userRep = new UserRepresentation();
                userRep.setRealmRoles(List.of(roleUpdate.getPersonnelRole().getKeycloakId()));
                user.update(userRep);
                t.complete();
            } catch (Exception e) {
                log.error("failed to update user role", e);
                t.fail(e);
            }
        }, result);
    }

    @Override
    public final void close() {
        keycloak.close();
    }
}
