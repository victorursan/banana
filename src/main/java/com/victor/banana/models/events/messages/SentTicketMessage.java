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
public class SentTicketMessage {
    private Long messageId;
    private Long chatId;
    private String ticketId;

    public SentTicketMessage(JsonObject jsonObject) {
        SentTicketMessageConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        SentTicketMessageConverter.toJson(this, json);
        return json;
    }

    public static SentTicketMessage fromJson(JsonObject jsonObject) {
        return new SentTicketMessage(jsonObject);
    }
}
