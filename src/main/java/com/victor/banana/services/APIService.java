package com.victor.banana.services;

import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.utils.SecurityUtils;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;

public abstract class APIService {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public final PersonnelService personnelService;

    public APIService(PersonnelService personnelService) {
        this.personnelService = personnelService;
    }

    abstract public Future<Router> routes(Future<OpenAPI3RouterFactory> routerFactoryFuture);

    protected final void isUserAuthorized(RoutingContext rc, SecurityUtils.Authority authority, Handler<Personnel> authorizedPersonnel) {
        SecurityUtils.isUserAuthorized(rc.user(), authority)
                .onSuccess(tUserOpt ->
                        tUserOpt.ifPresentOrElse(tUser -> {
                            Future.<Personnel>future(p -> personnelService.getOrElseCreatePersonnel(tUser, p))
                                    .onSuccess(authorizedPersonnel)
                                    .onFailure(failureHandler(rc, 500));
                        }, () -> rc.response().setStatusCode(401).end())
                )
                .onFailure(failureHandler(rc, 500));
    }

    protected Handler<Throwable> failureHandler(RoutingContext rc, int i) {
        return t -> {
            log.error(t.getMessage(), t);
            rc.response().setStatusCode(i).end(t.getMessage());
        };
    }
}
