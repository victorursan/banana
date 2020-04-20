package com.victor.banana.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public final class SerdesUtils {
    private static final Logger log = LoggerFactory.getLogger(SerdesUtils.class);

    public static void deserializeIntoObject(Object obj, JsonObject jsonObject) {
        try {
            DatabindCodec.mapper().readerForUpdating(obj).readValue(jsonObject.encode());
        } catch (JsonProcessingException e) {
            log.error("Failed to deserializeIntoObject", e);
        }
    }

    public static JsonObject serializeToJsonObject(Object obj) {
        try {
            return new JsonObject(DatabindCodec.mapper().writer().writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            log.error("Failed to serializeToJsonObject", e);
            return null;
        }
    }

}
