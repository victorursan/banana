package com.victor.banana.models.events.stickies;

import com.victor.banana.utils.Constants.PersonnelRole;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.List;
import java.util.Optional;
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
public class ActionUpdate {
    @NotNull
    private UUID id;
    @Builder.Default
    private Optional<String> action = Optional.empty();
    @Builder.Default
    private List<PersonnelRole> roles = List.of();

    public ActionUpdate(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
