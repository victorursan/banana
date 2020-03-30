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
public class SendTicketMessage {
    private Long chatId;
    private String ticketId;
    private String ticketMessage;

    public SendTicketMessage(JsonObject jsonObject) {
        SendTicketMessageConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        SendTicketMessageConverter.toJson(this, json);
        return json;
    }

    public static SendTicketMessage fromJson(JsonObject jsonObject) {
        return new SendTicketMessage(jsonObject);
    }
}
