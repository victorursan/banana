package com.victor.banana.models.configs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TelegramBotConfig {
    private String botUsername;
    private String botToken;
}
