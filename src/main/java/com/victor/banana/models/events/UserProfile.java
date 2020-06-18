package com.victor.banana.models.events;


import com.victor.banana.models.events.locations.Building;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.utils.Constants.PersonnelRole;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.Optional;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
@DataObject
public class UserProfile {
    @NotNull
    private Personnel personnel;
    @Builder.Default
    private Optional<PersonnelRole> role = Optional.empty();
    @Builder.Default
    private Optional<Building> building = Optional.empty();

    public UserProfile(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }

}
