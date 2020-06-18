package com.victor.banana.models.events.stickies;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.Optional;
import java.util.UUID;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Getter
@DataObject
public class StickyLocationUpdate {
    @NotNull
    private UUID id;
    @Builder.Default
    private Optional<String> name = Optional.empty();
    @Builder.Default
    private Optional<UUID> floorId = Optional.empty();

    public StickyLocationUpdate(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
