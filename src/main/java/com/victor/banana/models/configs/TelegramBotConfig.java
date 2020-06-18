package com.victor.banana.models.configs;

import lombok.*;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class TelegramBotConfig {
    @NotNull
    private String botUsername;
    @NotNull
    private String botToken;
}
