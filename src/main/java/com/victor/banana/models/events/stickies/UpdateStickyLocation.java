package com.victor.banana.models.events.stickies;

import com.victor.banana.models.events.locations.StickyLocation;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.List;
import java.util.UUID;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@DataObject
public class UpdateStickyLocation {
    @Builder.Default
    private List<StickyLocation> add = List.of();
    @Builder.Default
    private List<StickyLocation> update = List.of();
    @Builder.Default
    private List<UUID> activate = List.of();
    @Builder.Default
    private List<UUID> remove = List.of();

    public UpdateStickyLocation(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
