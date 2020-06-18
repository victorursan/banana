package com.victor.banana.controllers.bot;

import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.messages.*;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.services.CartchufiService;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.victor.banana.controllers.bot.KeyboardOptions.getKeyboardFor;
import static com.victor.banana.controllers.bot.KeyboardOptions.getMessageStateForCallback;
import static com.victor.banana.utils.CallbackUtils.mergeFutures;
import static com.victor.banana.utils.CallbackUtils.sentCallback;
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
        commandsHandler.registerCommandWith("start", "start command", true, chat -> {

            final var createChannelMessage = CreateChannelMessage.builder()
                    .chatId(chat.getId())
                    .firstName(Optional.ofNullable(chat.getFirstName()))
                    .lastName(Optional.ofNullable(chat.getLastName()))
                    .username(String.format("@%s", chat.getUserName()))
                    .build();
            Future.<TelegramChannel>future(f -> cartchufiService.createChannel(createChannelMessage, f))
                    .onSuccess(telegramChannel -> log.debug("Created telegram channel"))
                    .onFailure(t -> log.error("Something went wrong creating telegram channel", t));
        });
        commandsHandler.registerCommandWith("my_tickets", "Here are all your acquired tickets. " +
                        "This means the rest of the team is expecting you to finish the task. " +
                        "You can press *Resolve* to complete them or *Back* to set them back to open.", false,
                chat -> cartchufiService.requestPersonnelTicketsInState(chat.getId(), TicketState.ACQUIRED));
        commandsHandler.registerCommandWith("open_tickets", "Here are all the open ticket in your building. " +
                        "Press *Acquire* to inform the rest of the team that you are responsible for this task.", false,
                chat -> cartchufiService.requestPersonnelTicketsInState(chat.getId(), TicketState.PENDING));
        commandsHandler.registerCommandWith("check_in", "Check-in to start receiving tickets in your building.", false,
                chat -> cartchufiService.checkIn(chat.getId()));
        commandsHandler.registerCommandWith("check_out", "Check-out to stop receiving tickets in your building.", false,
                chat -> cartchufiService.checkOut(chat.getId()));

        executeMessages(List.of(new SetMyCommands(commandsHandler.getBotCommands())))
                .onFailure(t -> log.error("failed register commands", t));
    }


    @Override
    @NotNull
    public void onUpdateReceived(Update update) {
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

    @NotNull
    public Future<List<SentTicketMessage>> sendMessages(List<SendTicketMessage> sendTicketMessages) {
        return executeMessages(sendTicketMessages,
                stm -> new SendMessage()
                        .setChatId(stm.getChatId())
                        .enableHtml(true)
                        .setText(stm.getTicketMessage())
                        .setReplyMarkup(getKeyboardFor(stm.getTicketState())),
                (stm, recvM) -> SentTicketMessage.builder()
                        .chatId(recvM.getChatId())
                        .messageId(recvM.getMessageId().longValue())
                        .ticketId(stm.getTicketId())
                        .build())
                .onComplete(e -> log.info(e.toString()));
    }

    @NotNull
    public Future<List<SentUpdateMessage>> updateMessages(List<SendUpdateMessage> sendUpdateMessages) {
        return executeMessages(sendUpdateMessages,
                sum -> new EditMessageText()
                        .setChatId(sum.getChatId())
                        .enableHtml(true)
                        .setText(sum.getText())
                        .setMessageId(sum.getMessageId().intValue())
                        .setReplyMarkup(getKeyboardFor(sum.getState())),
                (sum, recvM) ->
                        SentUpdateMessage.builder()
                                .chatId(sum.getChatId())
                                .messageId(sum.getMessageId())
                                .state(sum.getState())
                                .text(sum.getText())
                                .build() //todo
        );
    }

    @NotNull
    public Future<List<SentDeleteMessage>> deleteMessages(List<SendDeleteMessage> sendDeleteMessages) {
        return executeMessages(sendDeleteMessages,
                sdm -> new DeleteMessage()
                        .setChatId(sdm.getChatId())
                        .setMessageId(sdm.getMessageId().intValue()),
                (sdm, wasDeleted) ->
                        SentDeleteMessage.builder()
                                .chatId(sdm.getChatId())
                                .messageId(sdm.getMessageId())
                                .wasDeleted(wasDeleted)
                                .build()
        );
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
                    .channel(CreateChannelMessage.builder()
                            .chatId(updateMessage.getChatId())
                            .firstName(Optional.ofNullable(user.getFirstName()))
                            .lastName(Optional.ofNullable(user.getLastName()))
                            .username(String.format("@%s", user.getUserName()))
                            .build())
                    .messageId(updateMessage.getMessageId().longValue())
                    .message(updateMessage.getText())
                    .build();
            cartchufiService.receivedPersonnelMessage(personnelMessage);
        } else if (update.hasCallbackQuery()) {
            final var callbackquery = update.getCallbackQuery();
            final var callbackqueryMessage = callbackquery.getMessage();
            final var chatId = callbackqueryMessage.getChatId();
            final var messageId = callbackqueryMessage.getMessageId();
            final var messageStateOpt = getMessageStateForCallback(callbackquery.getData());
            executeMessages(List.of(new AnswerCallbackQuery().setCallbackQueryId(callbackquery.getId())))
                    .onFailure(t -> log.error("Failed to answer callback", t));
            messageStateOpt.ifPresentOrElse(messageState -> {
                final var recvUpdateMessage = RecvUpdateMessage.builder()
                        .messageId(messageId.longValue())
                        .chatId(chatId)
                        .state(messageState)
                        .build();
                cartchufiService.receivedMessageUpdate(recvUpdateMessage);
            }, () -> log.error("Invalid message state."));

        }
    }

    @NotNull
    private <T extends Serializable, M extends BotApiMethod<T>, E, R> Future<List<R>> executeMessages(List<E> elements,
                                                                                                      Function<E, M> mapper,
                                                                                                      BiFunction<E, T, R> resultMapper) {
        final var recvMessages = elements.stream().flatMap(el -> {
            final var sendMethod = mapper.apply(el);
            try {
                final var promise = Promise.<T>promise();
                executeAsync(sendMethod, sentCallback(promise));
                return Stream.of(promise.future().map(t -> resultMapper.apply(el, t)));
            } catch (TelegramApiException e) {
                log.error("failed to send message", e);
                return Stream.of(Future.<R>failedFuture(e));
            }
        }).collect(Collectors.toList());
        return mergeFutures(recvMessages);
    }

    @NotNull
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
