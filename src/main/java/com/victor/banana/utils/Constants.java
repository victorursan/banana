package com.victor.banana.utils;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

public final class Constants {
    private static final Logger log = LoggerFactory.getLogger(Constants.class);

    private Constants() {
    }

    public enum PersonnelRole {
        ADMIN(UUID.fromString("53e07fd5-8deb-4ab6-aedb-cbcdcf28eec1"), 0, "ADMIN", "admin"),
        COMMUNITY(UUID.fromString("90642ef3-cd01-4fe5-a789-af915ddeaebc"), 1, "COMMUNITY", "community"),
        CLEANER(UUID.fromString("56841b70-d343-445f-b4a7-c0b10ea4e0f6"), 2, "CLEANER", "cleaner"),
        MAINTENANCE(UUID.fromString("2a53b2dc-11c3-4de6-a382-b6a9a1e3173e"), 2, "MAINTENANCE", "maintenance"),
        MEMBER(UUID.fromString("8981b593-6d7a-45db-bbbe-cbcdd23cc693"), 3, "MEMBER", "member"),
        NO_ROLE(UUID.fromString("2fdeaa40-1e25-4b08-b960-5add7c18d59f"), 9, "NO ROLE");

        private final UUID uuid;
        private final int position;
        private final String name;
        private final Optional<String> keycloakId;

        PersonnelRole(UUID uuid, int position, String name) {
            this.uuid = uuid;
            this.position = position;
            this.name = name;
            this.keycloakId = Optional.empty();
        }

        PersonnelRole(UUID uuid, int position, String name, String keycloakId) {
            this.uuid = uuid;
            this.position = position;
            this.name = name;
            this.keycloakId = Optional.of(keycloakId);
        }

        public static Optional<PersonnelRole> from(UUID uuid) {
            for (PersonnelRole p : PersonnelRole.values()) {
                if (p.uuid.equals(uuid)) {
                    return Optional.of(p);
                }
            }
            log.error(String.format("Failed to find personnel role for id: [%s].", uuid));
            return Optional.empty();
        }

        public boolean isBetter(PersonnelRole otherRole) {
            return this.position < otherRole.position;
        }

        public String getName() {
            return name;
        }

        public Optional<String> getKeycloakId() {
            return keycloakId;
        }

        public UUID getUuid() {
            return uuid;
        }
    }

    public static class DBConstants {
        public static final UUID NO_LOCATION = UUID.fromString("95c12221-2314-4d1f-bf25-bd30d969c49f");
        public static final UUID NO_PERSONNEL = UUID.fromString("cf338d20-073a-4f28-ad68-a104d02eef9d");
    }

    public static class EventbusAddress {
        public static final String DATABASE = "eventbuss.service.database";
        public static final String KEYCLOAK_CLIENT = "eventbuss.service.keycloak.client";
        public static final String TELEGRAM_BOT = "eventbuss.service.telegram.bot";
        public static final String CARTCHUFI_ENGINE = "eventbuss.service.cartchufi.engine";
    }

    public static class DeliveryOptionsConstants {
        public static final DeliveryOptions LOCAL_DELIVERY = new DeliveryOptions().setLocalOnly(true);
    }
}
