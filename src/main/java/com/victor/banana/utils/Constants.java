package com.victor.banana.utils;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.jetbrains.annotations.NotNull;

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
        MEMBER(UUID.fromString("8981b593-6d7a-45db-bbbe-cbcdd23cc693"), 3, "MEMBER", "member");

        @NotNull
        private final UUID uuid;

        private final int position;
        @NotNull
        private final String name;
        @NotNull
        private final String keycloakId;

        PersonnelRole(UUID uuid, int position, String name, String keycloakId) {
            this.uuid = uuid;
            this.position = position;
            this.name = name;
            this.keycloakId = keycloakId;
        }

        @NotNull
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

        @NotNull
        public String getName() {
            return name;
        }

        @NotNull
        public String getKeycloakId() {
            return keycloakId;
        }

        @NotNull
        public UUID getUuid() {
            return uuid;
        }
    }


    public static class EventbusAddress {
        @NotNull
        public static final String DATABASE = "eventbuss.service.database";
        @NotNull
        public static final String KEYCLOAK_CLIENT = "eventbuss.service.keycloak.client";
        @NotNull
        public static final String TELEGRAM_BOT = "eventbuss.service.telegram.bot";
        @NotNull
        public static final String BOOKING = "eventbuss.service.booking";
        @NotNull
        public static final String PERSONNEL = "eventbuss.service.personnel";
        @NotNull
        public static final String TICKETING = "eventbuss.service.ticketing";
    }

    public static class DeliveryOptionsConstants {
        @NotNull
        public static final DeliveryOptions LOCAL_DELIVERY = new DeliveryOptions().setLocalOnly(true);
    }
}
