package com.victor.banana.models.requests;


import lombok.*;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class UpdatePersonnelLocationReq {
    private UUID newLocation;
}
