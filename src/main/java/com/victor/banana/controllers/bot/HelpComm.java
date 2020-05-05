package com.victor.banana.controllers.bot;

import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.ICommandRegistry;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.helpCommand.HelpCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public final class HelpComm extends HelpCommand {
    private final ICommandRegistry commandRegistry;

    public HelpComm(ICommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        if (arguments.length > 0) {
            IBotCommand command = commandRegistry.getRegisteredCommand(arguments[0]);
            String reply = getManText(command);
            try {
                absSender.execute(new SendMessage(chat.getId(), reply).setParseMode("HTML"));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            String reply = getHelpText(commandRegistry);
            try {
                absSender.execute(new SendMessage(chat.getId(), reply).setParseMode("HTML"));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}
