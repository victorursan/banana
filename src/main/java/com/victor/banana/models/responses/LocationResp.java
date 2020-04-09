package com.victor.banana.models.responses;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@AllArgsConstructor
@Data
public class LocationResp {
    private UUID id;
    private UUID parentLocation;
    private String message;
}
