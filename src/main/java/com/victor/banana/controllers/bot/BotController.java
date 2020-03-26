package com.victor.banana.controllers.bot;

import com.victor.banana.controllers.bot.KeyboardOptions.*;
import com.victor.banana.models.events.*;
import com.victor.banana.services.CartchufiService;
import com.victor.banana.services.DatabaseService;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.victor.banana.controllers.bot.KeyboardOptions.CallbackData.callbackDataFromString;
import static com.victor.banana.controllers.bot.KeyboardOptions.*;
import static io.vertx.core.Future.failedFuture;

public class BotController extends TelegramLongPollingCommandBot {
    private final static Logger log = LoggerFactory.getLogger(BotController.class);

    private final String botUsername;
    private final String botToken;
    private final DatabaseService databaseService;
    private final CartchufiService cartchufiService;

    public BotController(String botUsername, String botToken, DatabaseService databaseService, CartchufiService cartchufiService) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.databaseService = databaseService;
        this.cartchufiService = cartchufiService;
    }


    @Override
    public void processNonCommandUpdate(Update update) {
        log.info(update.toString());
        if (update.hasMessage() && update.getMessage().hasText()) {
            final var chatId = update.getMessage().getChatId();
            final var user = update.getMessage().getFrom();
            final var personnel = Personnel.builder()
                    .id(UUID.randomUUID().toString())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .build();
            Future.<Boolean>future(e -> {
                databaseService.addPersonnel(personnel, e);
            }).map(personnel.getId())
                    .flatMap(personnelId -> {
                        final var chat = TelegramChannel.builder()
                                .chatId(chatId)
                                .personnelId(personnelId)
                                .username(user.getUserName())
                                .build();
                        return Future.<Boolean>future(e -> databaseService.addChat(chat, e));
                    }).flatMap(chatExists -> {
                if (chatExists) {
                    final var message = ChatMessage.builder()
                            .chatId(chatId)
                            .messageId(update.getMessage().getMessageId().longValue())
                            .message(update.getMessage().getText())
                            .build();
                    return Future.<Boolean>future(c -> databaseService.addMessage(message, c)).map(message);
                }
                return failedFuture("problem adding the chat");
            }).onFailure(t -> log.error("An error occurred: ", t))
                    .onSuccess(m -> log.info("Added message: " + m.toString()));

        } else if (update.hasCallbackQuery()) {
            final var callbackquery = update.getCallbackQuery();
            Future.<Ticket>future(ct -> databaseService.getTicketForMessage(callbackquery.getMessage().getChatId(), callbackquery.getMessage().getMessageId().longValue(), ct))
                    .flatMap(tick ->
                         Future.<TelegramChannel>future(channel -> databaseService.getChat(callbackquery.getMessage().getChatId(), channel))
                                .flatMap(tc -> {
                        final Optional<CallbackData> callbackData = callbackDataFromString(callbackquery.getData());
                        final var keyboard = callbackData.map(KeyboardOptions::getKeyboardFor).orElse(firstKeyboard());
                        final var isFirstKeyboard = keyboard.equals(firstKeyboard());
                        final var text = callbackData.flatMap(c -> getFormatedMessage(c, tc.getUsername()))
                                .map(c -> String.format("%s | %s", tick.getMessage(), c)).orElse(tick.getMessage());
                        final var editMarkup = new EditMessageText()
                                .setChatId(callbackquery.getMessage().getChatId().toString())
                                .setText(text)
                                .enableMarkdown(false)
                                .setMessageId(callbackquery.getMessage().getMessageId())
                                .setReplyMarkup(keyboard);

                        try {
                            final var something = execute(editMarkup);
                            if (something instanceof Message) {
                                final var message = ((Message) something);
                                return Future.<List<ChatTicketMessage>>future(t -> cartchufiService.messageAcquired(message.getChatId(), message.getMessageId().longValue(), t))
                                        .onSuccess(chats -> chats.forEach(chat -> {
                                            final var smth = new EditMessageText()
                                                    .setChatId(chat.getChatId())
                                                    .setText(text)
                                                    .enableMarkdown(true)
                                                    .setMessageId(chat.getMessageId().intValue())
                                                    .setReplyMarkup(isFirstKeyboard ? firstKeyboard() : noKeyboard());
                                            try {
                                                execute(smth);
                                            } catch (TelegramApiException e) {
                                                e.printStackTrace();
                                            }
                                        }));


                            }
                            return Future.failedFuture("");
                        } catch (TelegramApiException e) {
                            return Future.failedFuture(e);
                        }
                    }))
                    .onFailure(t -> log.error(t.getMessage(), t));


        }
    }

    public Future<Void> broadcastMessage(Ticket ticket) {
        return Future.future(databaseService::getChats)
                .flatMap(chatIds -> {
                            final List<ChatTicketMessage> messages = chatIds.stream().flatMap(id -> {
                                final SendMessage sendMessage = new SendMessage() // Create a SendMessage object with mandatory fields
                                        .setChatId(id)
                                        .setText(ticket.getMessage())
                                        .setReplyMarkup(firstKeyboard());

                                try {
                                    Message message = execute(sendMessage);
                                    return Stream.of(ChatTicketMessage.builder()
                                            .chatId(id)
                                            .messageId(message.getMessageId().longValue())
                                            .ticketId(ticket.getId())
                                            .build());
                                } catch (TelegramApiException e) {
                                    log.error("failed to send message", e);
                                    return Stream.empty();
                                }
                            }).collect(Collectors.toList());
                            return Future.<Boolean>future(f -> databaseService.addTicketsMessage(messages, f)); //todo
                        }
                )
                .onFailure(t -> log.error(t.getMessage(), t))
                .mapEmpty();

    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }


    @Override
    public String getBotToken() {
        return botToken;
    }
}
