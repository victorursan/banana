package com.victor.banana.models.configs;

import lombok.*;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class TelegramBotConfig {
    private String botUsername;
    private String botToken;
}
