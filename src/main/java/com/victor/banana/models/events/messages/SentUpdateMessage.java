package com.victor.banana.models.events.messages;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
@DataObject(generateConverter = true)
public class SentUpdateMessage {
    private Long chatId;
    private Long messageId;
    private String text;
    private TicketMessageState state;

    public SentUpdateMessage(JsonObject jsonObject) {
        SentUpdateMessageConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        SentUpdateMessageConverter.toJson(this, json);
        return json;
    }

    public static SendUpdateMessage fromJson(JsonObject jsonObject) {
        return new SendUpdateMessage(jsonObject);
    }
}
