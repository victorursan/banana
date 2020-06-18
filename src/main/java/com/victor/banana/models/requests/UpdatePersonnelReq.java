package com.victor.banana.models.requests;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class UpdatePersonnelReq {
    private Optional<UUID> newRole = Optional.empty();
    private Optional<UUID> newBuilding = Optional.empty();
}
