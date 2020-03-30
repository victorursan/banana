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
public class RecvPersonnelMessage {
    private String firstName;
    private String lastName;
    private String username;
    private Long messageId;
    private Long chatId;
    private String message;

    public RecvPersonnelMessage(JsonObject jsonObject) {
        RecvPersonnelMessageConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        RecvPersonnelMessageConverter.toJson(this, json);
        return json;
    }

    public static RecvPersonnelMessage fromJson(JsonObject jsonObject) {
        return new RecvPersonnelMessage(jsonObject);
    }
}
