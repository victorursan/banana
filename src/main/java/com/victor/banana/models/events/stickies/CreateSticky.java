package com.victor.banana.models.events.stickies;

import com.victor.banana.models.events.locations.CreateStickyLocation;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.List;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@DataObject
public class CreateSticky {
    @NotNull
    private String title;
    @Builder.Default
    private List<CreateAction> actions = List.of();
    @Builder.Default
    private List<CreateStickyLocation> locations = List.of();

    public CreateSticky(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
