package com.victor.banana.models.events.personnel;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.Optional;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@DataObject
public class PersonnelFilter {
    @Builder.Default
    private Optional<String> username = Optional.empty();
    @Builder.Default
    private Boolean operating = Boolean.TRUE;

    public PersonnelFilter(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
