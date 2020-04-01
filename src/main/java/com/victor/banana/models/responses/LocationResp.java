package com.victor.banana.models.responses;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
public class LocationResp {
    private String id;
    private String message;
}
