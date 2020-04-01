package com.victor.banana.models.requests;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class StickyReq {
    private String message;
    private List<String> actions;
    private List<String> locations;
}
