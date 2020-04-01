package com.victor.banana.models.requests;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ActionSelectedReq {
    private String actionId;
    private String locationId;
}
