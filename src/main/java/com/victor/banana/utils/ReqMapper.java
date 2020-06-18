package com.victor.banana.utils;

import com.victor.banana.models.events.ActionSelected;
import com.victor.banana.models.events.TelegramLoginData;
import com.victor.banana.models.events.UpdateTicketState;
import com.victor.banana.models.events.locations.*;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.personnel.UpdatePersonnel;
import com.victor.banana.models.events.stickies.*;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.models.requests.*;
import com.victor.banana.utils.Constants.PersonnelRole;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.victor.banana.models.events.tickets.TicketState.*;
import static com.victor.banana.utils.MappersHelper.mapTs;

public final class ReqMapper {

    private ReqMapper() {
    }

    public static Optional<TicketState> ticketStateDeserializer(String ticketState) {
        return switch (ticketState) {
            case "acquired" -> Optional.of(ACQUIRED);
            case "solved" -> Optional.of(SOLVED);
            case "pending" -> Optional.of(PENDING);
            default -> Optional.empty();
        };
    }

    public static Function<AddStickyLocationReq, CreateStickyLocation> createStickyLocationDeserializer() {
        return ls -> CreateStickyLocation.builder()
                .name(ls.getName())
                .floorId(ls.getFloorId())
                .build();
    }

    public static Function<ActionUpdateReq, ActionUpdate> actionUpdateDeserializer() {
        return as -> ActionUpdate.builder()
                .id(as.getId())
                .action(as.getAction())
                .roles(as.getRoles().stream().flatMap((UUID uuid) -> PersonnelRole.from(uuid).stream()).collect(Collectors.toList()))
                .build();
    }

    public static Function<ActionStickyReq, CreateAction> createActionDeserializer() {
        return as -> CreateAction.builder()
                .roles(as.getRoles())
                .message(as.getAction())
                .build();
    }

    public static Function<ActionSelectedReq, ActionSelected> actionSelectedDeserializer() {
        return actionSelectedReq -> ActionSelected.builder()
                .actionId(actionSelectedReq.getActionId())
                .locationId(actionSelectedReq.getLocationId())
                .build();
    }


    public static Function<AddStickyReq, CreateSticky> createStickyDeserializer() {
        return stickyReq -> CreateSticky.builder()
                .message(stickyReq.getMessage())
                .actions(mapTs(createActionDeserializer()).apply(stickyReq.getActions()))
                .locations(mapTs(createStickyLocationDeserializer()).apply(stickyReq.getLocations()))
                .build();
    }

    public static Function<AddCompanyReq, CreateCompany> createCompanyDeserializer() {
        return companyReq -> CreateCompany.builder()
                .name(companyReq.getName())
                .build();
    }

    public static Function<AddBuildingFloorsReq, CreateBuildingFloors> createBuildingFloorsDeserializer() {
        return buildingFloorsReq -> CreateBuildingFloors.builder()
                .building(createBuildingDeserializer().apply(buildingFloorsReq.getBuilding()))
                .floors(mapTs(createFloorDeserializer()).apply(buildingFloorsReq.getFloors()))
                .build();
    }

    public static Function<AddBuildingReq, CreateBuilding> createBuildingDeserializer() {
        return buildingReq -> CreateBuilding.builder()
                .companyId(buildingReq.getCompanyId())
                .name(buildingReq.getName())
                .build();
    }

    public static Function<AddFloorReq, CreateFloor> createFloorDeserializer() {
        return floorReq -> CreateFloor.builder()
                .name(floorReq.getName())
                .build();
    }


    public static Function<UpdatePersonnelReq, UpdatePersonnel> updatePersonnelDeserializer() {
        return updatePersonnelReq -> UpdatePersonnel.builder()
                .roleId(updatePersonnelReq.getNewRole())
                .buildingId(updatePersonnelReq.getNewBuilding())
                .build();
    }

    public static Function<UpdateStickyReq, UpdateSticky> updateStickyDeserializer() {
        return stickyReq -> UpdateSticky.builder()
                .message(stickyReq.getMessage())
                .active(stickyReq.getActive())
                .actions(stickyReq.getActions().map(sa -> UpdateStickyCreateAction.builder()
                        .add(mapTs(createActionDeserializer()).apply(sa.getAdd()))
                        .update(mapTs(actionUpdateDeserializer()).apply(sa.getUpdate()))
                        .activate(sa.getActivate())
                        .remove(sa.getRemove()).build()))
                .locations(stickyReq.getLocations().map(sl -> UpdateStickyCreateLocation.builder()
                        .add(mapTs(createStickyLocationDeserializer()).apply(sl.getAdd()))
                        .update(mapTs(stickyLocationUpdateDeserializer()).apply(sl.getUpdate()))
                        .activate(sl.getActivate())
                        .remove(sl.getRemove()).build()))
                .build();
    }

    @NotNull
    public static Function<TelegramLoginDataReq, TelegramLoginData> telegramLoginDataDeserializer(Personnel personnel) {
        return telegramReq -> {
        final var telegramUsername = telegramReq.getUsername().startsWith("@") ? telegramReq.getUsername() : "@" + telegramReq.getUsername();
        return TelegramLoginData.builder()
                .personnel(personnel)
                .chatId(telegramReq.getId())
                .username(telegramUsername)
                .firstName(telegramReq.getFirstName())
                .lastName(telegramReq.getLastName())
                .build();
        };
    }

    public static Function<UpdateTicketReq, UpdateTicketState> updateTicketStateDeserializer(Personnel personnel) {
        return updateTicketReq -> UpdateTicketState.builder()
                .newTicketState(ticketStateDeserializer(updateTicketReq.getNewState()).orElseThrow()) //todo
                .personnelId(personnel.getId())
                .build();
    }

    public static Function<StickyLocationUpdateReq, StickyLocationUpdate> stickyLocationUpdateDeserializer() {
        return as -> StickyLocationUpdate.builder()
                .id(as.getId())
                .name(as.getName())
                .floorId(as.getFloorId())
                .build();
    }


}
