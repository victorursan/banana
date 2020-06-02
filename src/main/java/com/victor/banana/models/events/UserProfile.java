package com.victor.banana.models.events;


import com.victor.banana.models.events.locations.Location;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.roles.Role;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;

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
    private Personnel personnel;
    private Role role;
    private Location location;

    public UserProfile(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }

}
