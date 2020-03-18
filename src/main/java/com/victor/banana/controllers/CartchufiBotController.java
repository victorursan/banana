package com.victor.banana.controllers;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashSet;
import java.util.Set;

public class CartchufiBotController extends TelegramLongPollingBot {
    private final Set<Long> chatIds = new HashSet<>();

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            System.out.println(update.getMessage().getText());
            chatIds.add(update.getMessage().getChatId());
        }
    }

    public void broadcastMessage(final String message) {
        for (Long id : chatIds) {
            SendMessage sendMessage = new SendMessage() // Create a SendMessage object with mandatory fields
                    .setChatId(id)
                    .setText(message);
            try {
                execute(sendMessage); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "";
    }

    @Override
    public String getBotToken() {
        return "";
    }
}
