package com.victor.banana.models.responses;

import lombok.*;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class PersonnelResp {
    private UUID id;
    private String firstName;
    private String lastName;
    private UUID locationId;
    private UUID roleId;
}
