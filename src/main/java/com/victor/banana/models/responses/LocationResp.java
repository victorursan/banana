package com.victor.banana.models.responses;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.*;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class LocationResp {
    private UUID id;
    private UUID parentLocation;
    private String message;
}
