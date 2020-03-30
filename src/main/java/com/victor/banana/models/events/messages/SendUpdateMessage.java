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
public class SendUpdateMessage {
    private Long chatId;
    private Long messageId;
    private String text;
    private TicketMessageState state;

    public SendUpdateMessage(JsonObject jsonObject) {
        SendUpdateMessageConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        SendUpdateMessageConverter.toJson(this, json);
        return json;
    }

    public static SendUpdateMessage fromJson(JsonObject jsonObject) {
        return new SendUpdateMessage(jsonObject);
    }
}
