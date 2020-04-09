package com.victor.banana.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor
@Data
public class StickyLocationResp {
    private UUID id;
    private UUID locationId;
    private String message;
    @Singular
    private List<ActionStickyResp> actions;
}
