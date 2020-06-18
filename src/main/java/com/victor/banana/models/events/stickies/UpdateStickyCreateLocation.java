package com.victor.banana.models.events.stickies;

import com.victor.banana.models.events.locations.CreateStickyLocation;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.List;
import java.util.UUID;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@Builder
@DataObject
public class UpdateStickyCreateLocation {
    @Builder.Default
    private List<CreateStickyLocation> add = List.of();
    @Builder.Default
    private List<StickyLocationUpdate> update = List.of();
    @Builder.Default
    private List<UUID> activate = List.of();
    @Builder.Default
    private List<UUID> remove = List.of();

    public UpdateStickyCreateLocation(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
