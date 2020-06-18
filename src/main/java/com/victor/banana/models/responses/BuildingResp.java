package com.victor.banana.models.responses;


import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class BuildingResp {
    @NotNull
    private UUID id;
    @NotNull
    private UUID companyId;
    @NotNull
    private String name;
    @NotNull
    private Boolean active;
}
