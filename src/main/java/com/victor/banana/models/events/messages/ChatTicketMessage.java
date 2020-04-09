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
public class ChatTicketMessage {
    private Long messageId;
    private Long chatId;
    private String ticketId;

    public ChatTicketMessage(JsonObject jsonObject) {
        ChatTicketMessageConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        ChatTicketMessageConverter.toJson(this, json);
        return json;
    }

    public static ChatTicketMessage fromJson(JsonObject jsonObject) {
        return new ChatTicketMessage(jsonObject);
    }
}
