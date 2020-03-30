package com.victor.banana.utils;

import io.vertx.core.eventbus.DeliveryOptions;

public final class Constants {
    private Constants() {}

    public static class EventbusAddress {
        public static final String DATABASE = "eventbuss.service.database";
        public static final String TELEGRAM_BOT = "eventbuss.service.telegram.bot";
        public static final String CARTCHUFI_ENGINE = "eventbuss.service.cartchufi.engine";
    }

    public static class DeliveryOptionsConstants {
        public static final DeliveryOptions LOCAL_DELIVERY = new DeliveryOptions().setLocalOnly(true);
    }
}
