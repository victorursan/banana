package com.victor.banana.models.events.stickies;

import com.victor.banana.models.events.CreateLocation;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@AllArgsConstructor
@Data
@DataObject(generateConverter = true)
public class CreateSticky {
    private String message;
    private List<CreateAction> actions;
    private List<CreateLocation> locations;

    public CreateSticky(JsonObject jsonObject) {
        CreateStickyConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        CreateStickyConverter.toJson(this, json);
        return json;
    }

    public static CreateSticky fromJson(JsonObject jsonObject) {
        return new CreateSticky(jsonObject);
    }
}
