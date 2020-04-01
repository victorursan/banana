package com.victor.banana.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Builder
@AllArgsConstructor
@Data
public class StickyLocationResp {
    private String id;
    private String locationId;
    private String message;
    @Singular
    private List<ActionStickyResp> actions;
}
