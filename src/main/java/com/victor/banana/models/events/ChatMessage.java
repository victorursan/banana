package com.victor.banana.models.events;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;

@Builder
@AllArgsConstructor
@Data
@DataObject(generateConverter = true)
public class ChatMessage {
    private Long messageId;
    private Long chatId;
    private String message;

    public ChatMessage(JsonObject jsonObject) {
        ChatMessageConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        ChatMessageConverter.toJson(this, json);
        return json;
    }

    public static ChatMessage fromJson(JsonObject jsonObject) {
        return new ChatMessage(jsonObject);
    }
}
