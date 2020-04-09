package com.victor.banana.models.requests;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class AddStickyReq {
    private String message;
    private List<ActionStickyReq> actions;
    private List<LocationStickyReq> locations;
}
