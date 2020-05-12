package com.victor.banana.controllers.bot;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.ICommandRegistry;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.helpCommand.HelpCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public final class HelpComm extends HelpCommand {
    private final Logger log = LoggerFactory.getLogger(HelpComm.class);
    private final ICommandRegistry commandRegistry;

    public HelpComm(ICommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        if (arguments.length > 0) {
            final var command = commandRegistry.getRegisteredCommand(arguments[0]);
            final var reply = getManText(command);
            sendMessage(absSender, chat, reply);
        } else {
            final var reply = getHelpText(commandRegistry);
            sendMessage(absSender, chat, reply);
        }
    }

    private void sendMessage(AbsSender absSender, Chat chat, String reply) {
        try {
            absSender.execute(new SendMessage(chat.getId(), reply).setParseMode("HTML"));
        } catch (TelegramApiException e) {
            log.error("Failed to send message", e);
        }
    }
}
