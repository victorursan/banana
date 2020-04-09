package com.victor.banana.models.requests;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class ActionStickyReq {
    private String action;
    private UUID roleId;
}
