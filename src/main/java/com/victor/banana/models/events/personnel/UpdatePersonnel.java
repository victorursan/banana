package com.victor.banana.models.events.personnel;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.Optional;
import java.util.UUID;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
@DataObject
public class UpdatePersonnel {
    @Builder.Default
    private Optional<String> firstName = Optional.empty();
    @Builder.Default
    private Optional<String> lastName = Optional.empty();
    @Builder.Default
    private Optional<String> email = Optional.empty();
    @Builder.Default
    private Optional<UUID> buildingId = Optional.empty();
    @Builder.Default
    private Optional<UUID> roleId = Optional.empty();

    public UpdatePersonnel(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
