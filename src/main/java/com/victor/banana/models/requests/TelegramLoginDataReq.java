package com.victor.banana.models.requests;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Optional;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class TelegramLoginDataReq {
    private Long id;
    private String username;
    private Optional<String> firstName = Optional.empty();
    private Optional<String> lastName = Optional.empty();
}
