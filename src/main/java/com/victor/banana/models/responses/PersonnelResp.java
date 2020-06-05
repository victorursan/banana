package com.victor.banana.models.responses;

import lombok.*;

import java.util.Optional;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class PersonnelResp {
    private UUID id;
    private Optional<String> firstName;
    private Optional<String> lastName;
    private Optional<String> email;
    private Optional<String> username;
    private UUID locationId;
    private UUID roleId;
}
