package com.victor.banana.controllers.db;

import com.victor.banana.models.events.locations.*;
import com.victor.banana.utils.CallbackUtils;
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor;
import io.vertx.core.Future;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static com.victor.banana.controllers.db.QueryHandler.*;
import static com.victor.banana.controllers.db.RowMappers.*;
import static com.victor.banana.jooq.Tables.*;
import static io.vertx.core.Future.succeededFuture;

public final class LocationQueryHandler {
    private LocationQueryHandler() {
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addBuildingFloorsQ(BuildingFloors buildingFloors) {
        return withTransaction(t -> {
            final var buildingF = addBuildingQ(buildingFloors.getBuilding()).apply(t);
            return buildingF.flatMap(ignore -> addFloorsQ(buildingFloors.getFloors()).apply(t));
        });
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addCompanyQ(Company company) {
        return execute(insertIntoCompany(company), 1, "add company");
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addBuildingQ(Building building) {
        return execute(insertIntoBuilding(building), 1, "add building");
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addFloorQ(Floor floor) {
        return execute(insertIntoFloor(floor), 1, "add floor");
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addFloorsQ(List<Floor> floors) {
        if(floors.size() == 0) {
            return t -> succeededFuture();
        }
        return execute(insertIntoFloor(floors), 1, "add floors");
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<BuildingLocations>> getBuildingLocationsForCompanyQ(UUID companyId) {
        return withTransaction(t -> {
            final var companyF = getCompanyWithIdQ(companyId).apply(t);
            final var buildingsF = getBuildingsForCompanyQ(companyId).apply(t);
            final var floorsF = getFloorsForCompanyQ(companyId).apply(t);
            return CallbackUtils.mergeFutures(companyF, buildingsF, floorsF)
                    .map(a -> BuildingLocations.builder()
                            .company(companyF.result())
                            .buildings(buildingsF.result())
                            .floors(floorsF.result())
                            .build()
                    );
        });
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<FloorLocations>> getFloorLocationsForBuildingQ(UUID buildingId) {
        return withTransaction(t -> {
            final var buildingF = getBuildingWithIdQ(buildingId).apply(t);
            final var floorsF = getFloorsForBuildingQ(buildingId).apply(t);
            final var stickyLocationsF = getStickyLocationsForBuildingIdQ(buildingId).apply(t);
            return CallbackUtils.mergeFutures(buildingF, floorsF, stickyLocationsF)
                    .map(a -> FloorLocations.builder()
                            .building(buildingF.result())
                            .floors(floorsF.result())
                            .stickyLocations(stickyLocationsF.result())
                            .build()
                    );
        });
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Company>> getCompanyWithIdQ(UUID companyId) {
        return findOne(selectCompanyWhere(COMPANY.COMPANY_ID.eq(companyId)),
                rowToCompany());
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Building>> getBuildingWithIdQ(UUID buildingId) {
        return findOne(selectBuildingWhere(BUILDING.BUILDING_ID.eq(buildingId)), rowToBuilding());
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<Building>>> getBuildingsForCompanyQ(UUID companyId) {
        return findMany(selectBuildingWhere(BUILDING.COMPANY_ID.eq(companyId)), rowToBuilding());
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<Floor>>> getFloorsForCompanyQ(UUID companyId) {
        return findMany(c -> selectFloor().apply(c)
                .innerJoin(BUILDING).using(FLOOR.BUILDING_ID)
                .where(BUILDING.COMPANY_ID.eq(companyId)), rowToFloor());
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<Floor>>> getFloorsForBuildingQ(UUID buildingId) {
        return findMany(selectFloorWhere(FLOOR.BUILDING_ID.eq(buildingId)), rowToFloor());
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<StickyLocation>>> getStickyLocationsForBuildingIdQ(UUID buildingId) {
        return findMany(c -> selectStickyLocation().apply(c)
                        .innerJoin(FLOOR).using(STICKY_LOCATION.FLOOR_ID)
                        .where(FLOOR.BUILDING_ID.eq(buildingId)),
                rowToStickyLocation());
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<StickyLocation>>> getStickyLocationsForStickyIdQ(UUID stickyId) {
        return findMany(selectStickyLocationWhere(STICKY_LOCATION.STICKY_ID.eq(stickyId)),
                rowToStickyLocation());
    }

    @NotNull
    private static Function<DSLContext, ResultQuery<Record5<UUID, UUID, UUID, String, Boolean>>> selectStickyLocationWhere(Condition condition) {
        return selectStickyLocation().andThen(s -> s.where(condition));
    }

    @NotNull
    private static Function<DSLContext, ResultQuery<Record4<UUID, UUID, String, Boolean>>> selectBuildingWhere(Condition condition) {
        return selectBuilding().andThen(s -> s.where(condition));
    }

    @NotNull
    private static Function<DSLContext, ResultQuery<Record4<UUID, UUID, String, Boolean>>> selectFloorWhere(Condition condition) {
        return selectFloor().andThen(s -> s.where(condition));
    }

    @NotNull
    private static Function<DSLContext, ResultQuery<Record3<UUID, String, Boolean>>> selectCompanyWhere(Condition condition) {
        return selectCompany().andThen(s -> s.where(condition));
    }

    @NotNull
    private static Function<DSLContext, SelectJoinStep<Record5<UUID, UUID, UUID, String, Boolean>>> selectStickyLocation() {
        return c -> c.select(STICKY_LOCATION.LOCATION_ID, STICKY_LOCATION.FLOOR_ID, STICKY_LOCATION.STICKY_ID, STICKY_LOCATION.NAME, STICKY_LOCATION.ACTIVE)
                .from(STICKY_LOCATION);
    }

    @NotNull
    private static Function<DSLContext, SelectJoinStep<Record4<UUID, UUID, String, Boolean>>> selectFloor() {
        return c -> c.select(FLOOR.FLOOR_ID, FLOOR.BUILDING_ID, FLOOR.NAME, FLOOR.ACTIVE)
                .from(FLOOR);
    }

    @NotNull
    private static Function<DSLContext, SelectJoinStep<Record4<UUID, UUID, String, Boolean>>> selectBuilding() {
        return c -> c.select(BUILDING.BUILDING_ID, BUILDING.COMPANY_ID, BUILDING.NAME, BUILDING.ACTIVE)
                .from(BUILDING);
    }

    @NotNull
    private static Function<DSLContext, SelectJoinStep<Record3<UUID, String, Boolean>>> selectCompany() {
        return c -> c.select(COMPANY.COMPANY_ID, COMPANY.NAME, COMPANY.ACTIVE)
                .from(COMPANY);
    }

    @NotNull
    private static Function<DSLContext, Query> insertIntoCompany(Company company) {
        return c -> c.insertInto(COMPANY, COMPANY.COMPANY_ID, COMPANY.NAME, COMPANY.ACTIVE)
                .values(company.getId(), company.getName(), company.getActive());
    }

    @NotNull
    private static Function<DSLContext, Query> insertIntoBuilding(Building building) {
        return c -> c.insertInto(BUILDING, BUILDING.BUILDING_ID, BUILDING.COMPANY_ID, BUILDING.NAME, BUILDING.ACTIVE)
                .values(building.getId(), building.getCompanyId(), building.getName(), building.getActive());
    }

    @NotNull
    private static Function<DSLContext, Query> insertIntoFloor(Floor floor) {
        return insertIntoFloor(List.of(floor));
    }

    @NotNull
    private static Function<DSLContext, Query> insertIntoFloor(List<Floor> floors) {
        return c -> {
            final var insert = c.insertInto(FLOOR, FLOOR.FLOOR_ID, FLOOR.BUILDING_ID, FLOOR.NAME, FLOOR.ACTIVE);
            floors.forEach(floor -> insert.values(floor.getId(), floor.getBuildingId(), floor.getName(), floor.getActive()));
            return insert;
        };
    }
}
