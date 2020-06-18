package com.victor.banana.models.requests;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class ActionUpdateReq {
    private UUID id;
    private Optional<String> action = Optional.empty();
    private List<UUID> roles = List.of();
}
