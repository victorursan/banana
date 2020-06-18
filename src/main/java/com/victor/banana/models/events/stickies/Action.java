package com.victor.banana.models.events.stickies;

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
public class Action {
    @NotNull
    private UUID id;
    @NotNull
    private UUID stickyId;
    @NotNull
    private String name;
    @NotNull
    private String description;
    @NotNull
    private Boolean active;
    @Builder.Default
    private List<UUID> roles = List.of();
    @Builder.Default
    private ActionState state = ActionState.AVAILABLE; //todo

    public Action(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
