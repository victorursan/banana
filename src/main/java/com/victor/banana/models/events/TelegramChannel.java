package com.victor.banana.models.events;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
@DataObject(generateConverter = true)
public class TelegramChannel {
    private Long chatId;
    private String personnelId;
    private String username;

    public TelegramChannel(JsonObject jsonObject) {
        TelegramChannelConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        TelegramChannelConverter.toJson(this, json);
        return json;
    }

    public static TelegramChannel fromJson(JsonObject jsonObject) {
        return new TelegramChannel(jsonObject);
    }

}
