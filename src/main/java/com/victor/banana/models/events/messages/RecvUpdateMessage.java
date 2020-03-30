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
public class RecvUpdateMessage {
    private Long chatId;
    private Long messageId;
    private TicketMessageState state;

    public RecvUpdateMessage(JsonObject jsonObject) {
        RecvUpdateMessageConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        RecvUpdateMessageConverter.toJson(this, json);
        return json;
    }

    public static RecvUpdateMessage fromJson(JsonObject jsonObject) {
        return new RecvUpdateMessage(jsonObject);
    }
}
