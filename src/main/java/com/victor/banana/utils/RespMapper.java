package com.victor.banana.utils;

import com.victor.banana.models.events.UserProfile;
import com.victor.banana.models.events.locations.*;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.stickies.Action;
import com.victor.banana.models.events.stickies.ActionState;
import com.victor.banana.models.events.stickies.ScanSticky;
import com.victor.banana.models.events.stickies.Sticky;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.models.responses.*;
import com.victor.banana.utils.Constants.PersonnelRole;

import java.util.function.Function;

import static com.victor.banana.utils.MappersHelper.mapTs;

public final class RespMapper {

    private RespMapper() {
    }

    public static Function<FloorLocations, FloorLocationsResp> floorLocationsSerializer() {
        return l -> FloorLocationsResp.builder()
                .building(buildingSerializer().apply(l.getBuilding()))
                .floors(mapTs(floorSerializer()).apply(l.getFloors()))
                .stickyLocations(mapTs(stickyLocationSerializer()).apply(l.getStickyLocations()))
                .build();
    }

    public static Function<BuildingLocations, BuildingLocationsResp> buildingLocationsSerializer() {
        return l -> BuildingLocationsResp.builder()
                .company(companyBuildingSerializer().apply(l.getCompany()))
                .buildings(mapTs(buildingSerializer()).apply(l.getBuildings()))
                .floors(mapTs(floorSerializer()).apply(l.getFloors()))
                .build();
    }

    public static Function<Building, BuildingResp> buildingSerializer() {
        return l -> BuildingResp.builder()
                .id(l.getId())
                .name(l.getName())
                .companyId(l.getCompanyId())
                .active(l.getActive())
                .build();
    }

    public static Function<Floor, FloorResp> floorSerializer() {
        return l -> FloorResp.builder()
                .id(l.getId())
                .name(l.getName())
                .buildingId(l.getBuildingId())
                .active(l.getActive())
                .build();
    }

    public static Function<Company, CompanyResp> companyBuildingSerializer() {
        return l -> CompanyResp.builder()
                .id(l.getId())
                .name(l.getName())
                .build();
    }

    public static Function<BuildingFloors, BuildingFloorsResp> buildingFloorsSerializer() {
        return l -> BuildingFloorsResp.builder()
                .building(buildingSerializer().apply(l.getBuilding()))
                .floors(mapTs(floorSerializer()).apply(l.getFloors()))
                .build();
    }

    public static Function<StickyLocation, StickyLocationResp> stickyLocationSerializer() {
        return l -> StickyLocationResp.builder()
                .id(l.getId())
                .name(l.getName())
                .floorId(l.getFloorId())
                .stickyId(l.getStickyId())
                .active(l.getActive())
                .build();
    }

    public static Function<PersonnelRole, RoleResp> roleSerializer() {
        return r -> RoleResp.builder()
                .id(r.getUuid())
                .role(r.getName())
                .build();
    }

    public static Function<Personnel, PersonnelResp> personnelSerializer() {
        return p -> PersonnelResp.builder()
                .id(p.getId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .email(p.getEmail())
                .username(p.getTelegramUsername())
                .buildingId(p.getBuildingId())
                .roleId(p.getRole().map(PersonnelRole::getUuid))
                .build();
    }


    public static Function<Ticket, TicketResp> ticketSerializer() {
        return ticketSerializer(false);
    }

    public static Function<Ticket, TicketResp> ticketSerializer(boolean full) {
        return t -> {
            final var ticketRespB = TicketResp.builder()
                    .ticketId(t.getId())
                    .message(t.getMessage())
                    .state(ticketStateSerializer(t.getState()))
                    .createdAt(t.getCreatedAt());
            if (full) {
                ticketRespB.acquiredAt(t.getAcquiredAt())
                        .solvedAt(t.getSolvedAt())
                        .ownedBy(t.getOwnedBy());
            }
            return ticketRespB.build();
        };
    }

    public static Function<ScanSticky, ScanStickyResp> scanStickySerializer() {
        return s -> ScanStickyResp.builder()
                .id(s.getId())
                .message(s.getMessage())
                .actions(mapTs(actionStickySerializer()).apply(s.getActions()))
                .locationId(s.getLocationId())
                .build();
    }

    public static Function<Action, ActionStickyResp> actionStickySerializer() {
        return a -> ActionStickyResp.builder()
                .id(a.getId())
                .roles(a.getRoles())
                .message(a.getName())
                .state(actionStateSerializer(a.getState()))
                .build();
    }

    public static Function<Sticky, StickyResp> stickySerializer() {
        return sticky -> StickyResp.builder()
                .id(sticky.getId())
                .message(sticky.getTitle())
                .actions(mapTs(actionStickySerializer()).apply(sticky.getActions()))
                .locations(mapTs(stickyLocationSerializer()).apply(sticky.getStickyLocations()))
                .build();
    }

    public static Function<UserProfile, UserProfileResp> userProfileSerializer() {
        return userProfile -> UserProfileResp.builder()
                .id(userProfile.getPersonnel().getId())
                .email(userProfile.getPersonnel().getEmail())
                .firstName(userProfile.getPersonnel().getFirstName())
                .lastName(userProfile.getPersonnel().getLastName())
                .role(userProfile.getRole().map(roleSerializer()))
                .building(userProfile.getBuilding().map(buildingSerializer()))
                .telegramUsername(userProfile.getPersonnel().getTelegramUsername())
                .build();
    }

    public static String ticketStateSerializer(TicketState ticketState) {
        return switch (ticketState) {
            case ACQUIRED -> "acquired";
            case SOLVED -> "solved";
            case PENDING -> "pending";
        };
    }

    public static String actionStateSerializer(ActionState actionState) {
        return switch (actionState) {
            case AVAILABLE -> "available";
            case IN_PROGRESS -> "in_progress";
        };
    }
}
