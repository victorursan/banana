package com.victor.banana.utils;

import io.vertx.core.eventbus.DeliveryOptions;

import java.util.UUID;

public final class Constants {
    private Constants() {
    }

    public static class DBConstants {
        public static final UUID NO_ROLE = UUID.fromString("2fdeaa40-1e25-4b08-b960-5add7c18d59f");
        public static final UUID NO_LOCATION = UUID.fromString("95c12221-2314-4d1f-bf25-bd30d969c49f");
    }

    public static class EventbusAddress {
        public static final String DATABASE = "eventbuss.service.database";
        public static final String TELEGRAM_BOT = "eventbuss.service.telegram.bot";
        public static final String CARTCHUFI_ENGINE = "eventbuss.service.cartchufi.engine";
    }

    public static class DeliveryOptionsConstants {
        public static final DeliveryOptions LOCAL_DELIVERY = new DeliveryOptions().setLocalOnly(true);
    }
}
