package com.victor.banana.models.events.stickies;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.Optional;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@Builder
@DataObject
public class UpdateSticky {
    @Builder.Default
    private Optional<String> title = Optional.empty();
    @Builder.Default
    private Optional<Boolean> active = Optional.empty();
    @Builder.Default
    private Optional<UpdateStickyCreateAction> actions = Optional.empty();
    @Builder.Default
    private Optional<UpdateStickyCreateLocation> locations = Optional.empty();

    public UpdateSticky(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
