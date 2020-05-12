package com.victor.banana.utils;

import com.victor.banana.models.events.TokenUser;
import com.victor.banana.utils.Constants.PersonnelRole;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.impl.OAuth2TokenImpl;
import io.vertx.ext.auth.oauth2.impl.OAuth2UserImpl;

import java.util.Optional;



public final class SecurityUtils {
    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);

    public enum Authority {
        MEMBER("realm:member"),
        COMMUNITY("realm:community"),
        ADMIN("realm:admin");

        private final String value;

        Authority(String value) {
            this.value = value;
        }

        public PersonnelRole toPersonnelRole() {
            return switch (this) {
                case MEMBER -> PersonnelRole.MEMBER;
                case COMMUNITY -> PersonnelRole.COMMUNITY;
                case ADMIN -> PersonnelRole.ADMIN;
            };
        }
    }

    public static Future<Optional<TokenUser>> isUserAuthorized(User user, Authority authority) {
        final var oAuth2Token = (OAuth2TokenImpl) user;
        final var tokenUser = new TokenUser(oAuth2Token.accessToken());
        tokenUser.setAuthority(authority);
        return Future.<Boolean>future(f -> oAuth2Token.isAuthorized(authority.value, f))
                .map(isAuthorized -> isAuthorized ? Optional.of(tokenUser) : Optional.empty());
    }
}


