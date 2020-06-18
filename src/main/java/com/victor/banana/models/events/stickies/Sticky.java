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
public class Sticky {
    @NotNull
    private UUID id;
    @NotNull
    private String title;
    @NotNull
    private Boolean active;
    @Builder.Default
    private List<Action> actions = List.of();
    @Builder.Default
    private List<StickyLocation> stickyLocations = List.of();

    public Sticky(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
