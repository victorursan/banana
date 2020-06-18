package com.victor.banana.utils;


import com.victor.banana.actions.TicketAction;
import com.victor.banana.models.events.*;
import com.victor.banana.models.events.locations.*;
import com.victor.banana.models.events.messages.*;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.personnel.UpdatePersonnel;
import com.victor.banana.models.events.stickies.*;
import com.victor.banana.models.events.tickets.NotificationType;
import com.victor.banana.models.events.tickets.TicketNotification;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.models.requests.TelegramLoginDataReq;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static com.victor.banana.utils.MappersHelper.mapTs;
import static java.util.stream.Collectors.toList;

public final class CreateMappers {
    private CreateMappers() {
    }

    @NotNull
    public static KeyUserDelete createKeyUserDelete(String personnelId) {
        return KeyUserDelete.builder().personnelId(UUID.fromString(personnelId)).build();
    }

    @NotNull
    public static KeyUserRoleUpdate createKeyUserRoleUpdate(UUID userId, Constants.PersonnelRole role) {
        return KeyUserRoleUpdate.builder()
                .personnelId(userId)
                .personnelRole(role)
                .build();
    }

    @NotNull
    public static StickyTitle createStickyTitle(Sticky sticky, Optional<Boolean> status, Optional<String> title) {
        return StickyTitle.builder()
                .id(sticky.getId())
                .active(status.orElse(sticky.getActive()))
                .title(title.orElse(sticky.getTitle()))
                .build();
    }

    @NotNull
    public static SendUpdateMessage createSendUpdateMessage(TicketAction ticketAction, SentDeleteMessage sdm) {
        return SendUpdateMessage.builder()
                .chatId(sdm.getChatId())
                .messageId(sdm.getMessageId())
                .state(Optional.empty())
                .text(String.format("<s>%s</s>", StringEscapeUtils.escapeHtml4(ticketAction.getMessage())))
                .build();
    }

    @NotNull
    public static UpdateStickyAction createUpdateStickyAction(Sticky s, UpdateStickyCreateAction usa) {
        return UpdateStickyAction.builder()
                .add(mapTs(createActionToAction(s.getId())).apply(usa.getAdd()))
                .update(updateActions(s.getId(), usa.getUpdate(), s.getActions()))
                .activate(usa.getActivate())
                .remove(usa.getRemove())
                .build();
    }

    @NotNull
    public static UpdateStickyLocation createUpdateStickyLocation(Sticky s, UpdateStickyCreateLocation usl) {
        return UpdateStickyLocation.builder()
                .add(mapTs(createStickyLocationToStickyLocation(s.getId())).apply(usl.getAdd()))
                .update(updateLocation(s.getId(), usl.getUpdate(), s.getStickyLocations()))
                .activate(usl.getActivate())
                .remove(usl.getRemove())
                .build();
    }

    @NotNull
    public static List<StickyLocation> updateLocation(UUID stickyId, List<StickyLocationUpdate> stickyLocationUpdates, List<StickyLocation> stickyLocations) {
        return stickyLocationUpdates.stream().flatMap(au ->
                stickyLocations.stream().filter(a -> a.getId().equals(au.getId())).limit(1).map(a ->
                        StickyLocation.builder()
                                .id(a.getId())
                                .floorId(au.getFloorId().orElse(a.getFloorId()))
                                .stickyId(stickyId)
                                .name(au.getName().orElse(a.getName()))
                                .build())
        ).collect(toList());
    }

    @NotNull
    public static List<Action> updateActions(UUID stickyId, List<ActionUpdate> actionUpdates, List<Action> actions) {
        return actionUpdates.stream().flatMap(au ->
                actions.stream().filter(a -> a.getId().equals(au.getId())).limit(1).map(a ->
                        Action.builder()
                                .id(a.getId())
                                .stickyId(stickyId)
                                .roles(au.getRoles().stream().map(Constants.PersonnelRole::getUuid).collect(toList()))
                                .name(au.getAction().orElse(a.getName()))
                                .build())
        ).collect(toList());
    }

    @NotNull
    public static Personnel createPersonnelFrom(UpdatePersonnel updatePersonnel, Personnel p, UUID userId) {
        return Personnel.builder()
                .id(userId)
                .firstName(updatePersonnel.getFirstName().or(p::getFirstName))
                .lastName(updatePersonnel.getLastName().or(p::getLastName))
                .email(updatePersonnel.getEmail().or(p::getEmail))
                .role(updatePersonnel.getRoleId().flatMap(Constants.PersonnelRole::from).or(p::getRole))
                .buildingId(updatePersonnel.getBuildingId().or(p::getBuildingId))
                .chatId(p.getChatId())
                .build();
    }

    @NotNull
    public static Personnel createPersonnelFrom(TokenUser user) {
        return Personnel.builder()
                .id(user.getId())
                .firstName(Optional.ofNullable(user.getFirstName()))
                .lastName(Optional.ofNullable(user.getLastName()))
                .email(Optional.ofNullable(user.getEmail()))
                .role(Optional.of(user.getAuthority().toPersonnelRole()))
                .build();
    }

    @NotNull
    public static ChatMessage createChatMessage(RecvPersonnelMessage recvPersonnelMessage) {
        return ChatMessage.builder()
                .chatId(recvPersonnelMessage.getChannel().getChatId())
                .messageId(recvPersonnelMessage.getMessageId())
                .message(recvPersonnelMessage.getMessage())
                .build();
    }

    @NotNull
    public static Function<ChatTicketMessage, SendUpdateMessage> chatTicketMessageToSendUpdateMessage(TicketAction ticketAction) {
        return c -> SendUpdateMessage.builder()
                .chatId(c.getChatId())
                .messageId(c.getMessageId())
                .state(ticketAction.getMessageStateForChat(c.getChatId()))
                .text(StringEscapeUtils.escapeHtml4(ticketAction.getMessage()))
                .build();
    }

    @NotNull
    public static Function<ChatTicketMessage, SendDeleteMessage> chatTicketMessageToSendDeleteMessage() {
        return c -> SendDeleteMessage.builder()
                .chatId(c.getChatId())
                .messageId(c.getMessageId())
                .build();
    }

    @NotNull
    public static TicketNotification createTicketNotification(UUID personnelId, UUID ticketId) {
        return TicketNotification.builder()
                .ticketId(ticketId)
                .personnelId(personnelId)
                .type(NotificationType.CREATED_BY)
                .build();
    }

    @NotNull
    public static SendTicketMessage createTicketFrom(UUID ticketId, String message, Optional<TicketState> state, Long chatId) {
        return SendTicketMessage.builder()
                .chatId(chatId)
                .ticketId(ticketId)
                .ticketMessage(StringEscapeUtils.escapeHtml4(message))
                .ticketState(state)
                .build();
    }

    @NotNull
    public static UserProfile createUserProfileFrom(Personnel personnel) {
        return UserProfile.builder()
                .role(personnel.getRole())
                .personnel(personnel)
                .build();
    }

    @NotNull
    public static UserProfile createUserProfileFrom(Personnel personnel, Building buildingLocation) {
        return UserProfile.builder()
                .role(personnel.getRole())
                .building(Optional.of(buildingLocation))
                .personnel(personnel)
                .build();
    }

    @NotNull
    public static Personnel createPersonnelFrom(Optional<String> firstName, Optional<String> lastName) {
        return Personnel.builder()
                .id(UUID.randomUUID())
                .firstName(firstName)
                .lastName(lastName)
                .email(Optional.empty())
                .build();
    }

    @NotNull
    public static TelegramChannel createTelegramChannelFrom(UUID personnelId, Long chatId, String username) {
        return TelegramChannel.builder()
                .chatId(chatId)
                .personnelId(personnelId)
                .username(username)
                .build();
    }

    @NotNull
    public static Function<CreateBuildingFloors, BuildingFloors> createBuildingFloorsToBuildingFloors() {
        return cbf -> {
            final var building = createBuildingToBuilding().apply(cbf.getBuilding());
            return BuildingFloors.builder()
                    .building(building)
                    .floors(mapTs(createFloorToFloor(building.getId())).apply(cbf.getFloors()))
                    .build();
        };
    }

    @NotNull
    public static Function<CreateBuilding, Building> createBuildingToBuilding() {
        return createBuilding -> Building.builder()
                .id(UUID.randomUUID())
                .companyId(createBuilding.getCompanyId())
                .name(createBuilding.getName())
                .active(true)
                .build();
    }

    @NotNull
    public static Function<CreateCompany, Company> createCompanyToCompany() {
        return createCompany -> Company.builder()
                .id(UUID.randomUUID())
                .name(createCompany.getName())
                .active(true)
                .build();
    }

    @NotNull
    public static Function<CreateFloor, Floor> createFloorToFloor(UUID buildingId) {
        return createFloor -> Floor.builder()
                .id(UUID.randomUUID())
                .buildingId(buildingId)
                .name(createFloor.getName())
                .active(true)
                .build();
    }

    @NotNull
    public static Function<CreateSticky, Sticky> createStickyToSticky() {
        return createSticky -> {
            final var stickyId = UUID.randomUUID();
            return Sticky.builder()
                    .id(stickyId)
                    .title(createSticky.getMessage())
                    .active(true)
                    .actions(mapTs(createActionToAction(stickyId)).apply(createSticky.getActions()))
                    .stickyLocations(mapTs(createStickyLocationToStickyLocation(stickyId)).apply(createSticky.getLocations()))
                    .build();
        };
    }

    @NotNull
    public static Function<CreateStickyLocation, StickyLocation> createStickyLocationToStickyLocation(UUID stickyId) {
        return location -> StickyLocation.builder()
                .id(UUID.randomUUID())
                .floorId(location.getFloorId())
                .stickyId(stickyId)
                .name(location.getName())
                .active(true)
                .build();
    }

    @NotNull
    public static Function<CreateAction, Action> createActionToAction(UUID stickyId) {
        return action -> Action.builder()
                .id(UUID.randomUUID())
                .stickyId(stickyId)
                .roles(action.getRoles())
                .name(action.getMessage())
                .active(true)
                .build();
    }

}
