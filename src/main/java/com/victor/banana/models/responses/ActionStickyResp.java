package com.victor.banana.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@AllArgsConstructor
@Data
public class ActionStickyResp {
    private UUID id;
    private String message;
}
