package com.victor.banana.models.requests;


import lombok.*;
import lombok.NoArgsConstructor;

import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class UpdatePersonnelReq {
    private Optional<UUID> newRole;
    private Optional<UUID> newLocation;
}
