package com.victor.banana.controllers.bot;

import com.victor.banana.models.events.messages.*;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.services.CartchufiService;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.victor.banana.controllers.bot.KeyboardOptions.getKeyboardFor;
import static com.victor.banana.controllers.bot.KeyboardOptions.getMessageStateForCallback;
import static com.victor.banana.utils.CallbackUtils.mergeFutures;
import static java.util.function.Function.identity;

public final class BotController extends TelegramLongPollingBot {
    private final static Logger log = LoggerFactory.getLogger(BotController.class);

    private final String botUsername;
    private final String botToken;
    private final CartchufiService cartchufiService;
    private final CommandsHandler commandsHandler;

    public BotController(String botUsername, String botToken, CartchufiService cartchufiService) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.cartchufiService = cartchufiService;
        this.commandsHandler = new CommandsHandler(botUsername);
        commandsHandler.registerCommandWith("my_tickets", "The tickets you acquired and are not solved.",
                chat -> cartchufiService.requestPersonnelTicketsInState(chat.getId(), TicketState.ACQUIRED));
        commandsHandler.registerCommandWith("open_tickets", "All the tickets that are in pending state",
                chat -> cartchufiService.requestPersonnelTicketsInState(chat.getId(), TicketState.PENDING));
        commandsHandler.registerCommandWith("check_in", "This will enable receiving notifications",
                chat -> cartchufiService.checkIn(chat.getId()));
        commandsHandler.registerCommandWith("check_out", "This will disable receiving notifications",
                chat -> cartchufiService.checkOut(chat.getId()));

        executeMessages(List.of(new SetMyCommands(commandsHandler.getBotCommands())))
                .onFailure(t -> log.error("failed register commands", t));
    }

    @Override
    public final void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            final var message = update.getMessage();
            if (message.isCommand()) {
                if (!commandsHandler.executeCommand(this, message)) {
                    //we have received a not registered command, handle it as invalid
                    processInvalidCommandUpdate(update);
                }
                return;
            }
        }
        processNonCommandUpdate(update);
    }

    private void processInvalidCommandUpdate(Update update) {
        log.error(String.format("Received unknown update command(shouldn't happen): %s", update.toString()));
    }

    private void processNonCommandUpdate(Update update) {
        log.debug(update.toString());
        final var updateMessage = update.getMessage();
        if (update.hasMessage() && updateMessage.hasText()) {
            final var user = updateMessage.getFrom();
            final var personnelMessage = RecvPersonnelMessage.builder()
                    .chatId(updateMessage.getChatId())
                    .messageId(updateMessage.getMessageId().longValue())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .username(user.getUserName())
                    .message(updateMessage.getText())
                    .build();
            cartchufiService.receivedPersonnelMessage(personnelMessage);
        } else if (update.hasCallbackQuery()) {
            final var callbackquery = update.getCallbackQuery();
            final var callbackqueryMessage = callbackquery.getMessage();
            final var chatId = callbackqueryMessage.getChatId();
            final var messageId = callbackqueryMessage.getMessageId();
            final var messageState = getMessageStateForCallback(callbackquery.getData());
            final var recvUpdateMessage = RecvUpdateMessage.builder()
                    .messageId(messageId.longValue())
                    .chatId(chatId)
                    .state(messageState)
                    .build();
            cartchufiService.receivedMessageUpdate(recvUpdateMessage);
        }
    }

    public final Future<List<SentTicketMessage>> sendMessages(List<SendTicketMessage> sendTicketMessages) {
        return executeMessages(sendTicketMessages,
                stm -> new SendMessage()
                        .setChatId(stm.getChatId())
                        .setText(stm.getTicketMessage())
                        .setReplyMarkup(getKeyboardFor(stm.getTicketMessageState())),
                (stm, recvM) -> SentTicketMessage.builder()
                        .chatId(recvM.getChatId())
                        .messageId(recvM.getMessageId().longValue())
                        .ticketId(stm.getTicketId())
                        .build())
                .onComplete(e -> log.info(e.toString()));
    }

    public final Future<List<SentUpdateMessage>> updateMessages(List<SendUpdateMessage> sendUpdateMessages) {
        return executeMessages(sendUpdateMessages,
                smth -> new EditMessageText()
                        .setChatId(smth.getChatId())
                        .setText(smth.getText())
                        .setMessageId(smth.getMessageId().intValue())
                        .setReplyMarkup(getKeyboardFor(smth.getState())),
                (s, r) ->
                        SentUpdateMessage.builder()
                                .chatId(s.getChatId())
                                .messageId(s.getMessageId())
                                .state(s.getState())
                                .text(s.getText())
                                .build() //todo
        );
    }

    private <T extends Serializable, M extends BotApiMethod<T>, E, R> Future<List<R>> executeMessages(List<E> elements,
                                                                                                      Function<E, M> mapper,
                                                                                                      BiFunction<E, T, R> resultMapper) {
        final var recvMessages = elements.stream().flatMap(el -> {
            final var sendMethod = mapper.apply(el);
            try {
                final var f = Future.succeededFuture(execute(sendMethod)); //todo figure out `executeAsync`
                return Stream.of(f.map(t -> resultMapper.apply(el, t)));
            } catch (TelegramApiException e) {
                log.error("failed to send message", e);
                return Stream.of(Future.<R>failedFuture(e));
            }
        }).collect(Collectors.toList());
        return mergeFutures(recvMessages);
    }

    private <T extends Serializable, M extends BotApiMethod<T>> Future<List<T>> executeMessages(List<M> elements) {
        return executeMessages(elements, identity(), (a, b) -> b);
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
