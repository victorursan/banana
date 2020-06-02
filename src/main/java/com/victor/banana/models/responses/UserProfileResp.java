package com.victor.banana.models.responses;

import lombok.*;

import java.util.UUID;

@Builder
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class UserProfileResp {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private RoleResp role;
    private LocationResp location;
}
