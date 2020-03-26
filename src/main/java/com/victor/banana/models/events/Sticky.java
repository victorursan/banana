package com.victor.banana.models.events;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@Data
@DataObject(generateConverter = true)
public class Sticky {
    private String id;
    private String message;
    @Singular
    private List<Action> actions;

    public Sticky(JsonObject jsonObject) {
        StickyConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        StickyConverter.toJson(this, json);
        return json;
    }

    public static Sticky fromJson(JsonObject jsonObject) {
        return new Sticky(jsonObject);
    }

}
