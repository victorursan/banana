package com.victor.banana.controllers.bot;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.CommandRegistry;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.DefaultBotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class CommandsHandler { //todo should be a builder
    private static final Logger log = LoggerFactory.getLogger(CommandsHandler.class);
    private final CommandRegistry commandRegistry;
    private final Set<String> ignoredIdentifiers = new HashSet<>();

    public CommandsHandler(String botUsername) {
        this.commandRegistry = new CommandRegistry(false, botUsername);
        commandRegistry.register(new HelpComm(commandRegistry));
        commandRegistry.registerDefaultAction((sender, message)->
                log.error(String.format("unknown command: %s", message.getText())));
    }

    public final Optional<BotCommand> registerCommandWith(String name, String description, boolean hidden, Consumer<Chat> chatConsumer) {
        final var command = new DefaultBotCommand(name, description) {

            @Override
            public void execute(AbsSender absSender, User user, Chat chat, Integer messageId, String[] arguments) {
                chatConsumer.accept(chat);
            }
        };
        if (commandRegistry.register(command)) {
            if (hidden) {
                ignoredIdentifiers.add(command.getCommandIdentifier());
            }
            return Optional.of(command);
        }
        return Optional.empty();
    }

    public final boolean executeCommand(AbsSender absSender, Message message) {
        return commandRegistry.executeCommand(absSender, message);
    }

    public final List<org.telegram.telegrambots.meta.api.objects.commands.BotCommand> getBotCommands() {
        return commandRegistry.getRegisteredCommands().stream()
                .filter(i -> !ignoredIdentifiers.contains(i.getCommandIdentifier()))
                .map(bo -> new org.telegram.telegrambots.meta.api.objects.commands.BotCommand(bo.getCommandIdentifier(), bo.getDescription()))
                .collect(Collectors.toList());

    }




}
