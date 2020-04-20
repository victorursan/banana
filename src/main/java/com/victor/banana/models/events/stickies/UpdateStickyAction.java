package com.victor.banana.models.events.stickies;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;

import java.util.List;
import java.util.UUID;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@Builder
@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@DataObject
public class UpdateStickyAction {
    @Builder.Default
    private List<Action> add = List.of();
    @Builder.Default
    private List<UUID> activate = List.of();
    @Builder.Default
    private List<UUID> remove = List.of();

    public UpdateStickyAction(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
