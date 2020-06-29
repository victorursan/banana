package com.victor.banana.models.requests.booking;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class AddRoomReq {
    private UUID floorId;
    private String name;
    private String roomType;
    private Integer capacity;
}
