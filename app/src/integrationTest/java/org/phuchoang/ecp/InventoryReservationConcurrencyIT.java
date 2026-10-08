package org.phuchoang.ecp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.inventory.api.reservation.InsufficientStockException;
import org.phuchoang.ecp.inventory.api.reservation.InventoryReservationFacade;
import org.phuchoang.ecp.inventory.api.reservation.ReservationSet;
import org.phuchoang.ecp.inventory.api.reservation.ReserveStockCommand;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.repository.StockItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L5 / NFR-REL-03: races use PostgreSQL, never an in-memory approximation. This class owns its
 * Flyway/Hibernate schema so its fixture data cannot leak into another L5 class.
 */
@SpringBootTest(properties = {
    "ecp.cursor.active-key=integration-test-cursor-secret-0001",
    "spring.flyway.default-schema=inventory_l5",
    "spring.flyway.schemas=inventory_l5",
    "spring.jpa.properties.hibernate.default_schema=inventory_l5",
    // This isolated schema intentionally contains no other module's outbox table.
    "spring.task.scheduling.enabled=false",
    "spring.kafka.listener.auto-startup=false"
})
@Import(TestcontainersConfiguration.class)
class InventoryReservationConcurrencyIT {

    private static final Instant EXPIRY = Instant.parse("2026-10-05T00:00:00Z");

    @Autowired private InventoryReservationFacade reservations;
    @Autowired private StockItemRepository stockItems;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;

    private UUID warehouseId;

    @BeforeEach
    void seedWarehouse() {
        warehouseId = UUID.randomUUID();
        jdbc.update("insert into inventory_l5.inventory_warehouse (id, code, name, country_code) values (?, ?, ?, ?)",
            warehouseId, "L5-" + warehouseId.toString().substring(0, 8), "L5 test warehouse", "VN");
    }

    @Test
    void admitsExactlyAvailableUnitsWhenTwentyFourThreadsRaceOneSku() throws Exception {
        UUID itemId = seedStock("RACE-ONE", 8);

        List<Attempt> outcomes = race(24, () -> reserve(UUID.randomUUID(), itemId, "RACE-ONE", UUID.randomUUID()));

        assertThat(outcomes).filteredOn(Attempt::succeeded).hasSize(8);
        assertThat(outcomes).filteredOn(Attempt::shortfall).hasSize(16);
        assertThat(reserved(itemId)).isEqualTo(8);
        assertThat(onHand(itemId)).isEqualTo(8);
    }

    @Test
    void rollsBackTheOtherLineWhenMultiLineReservationsRace() throws Exception {
        UUID scarce = seedStock("RACE-SCARCE", 5);
        UUID abundant = seedStock("RACE-ABUNDANT", 24);

        List<Attempt> outcomes = race(24, () -> {
            UUID orderId = UUID.randomUUID();
            return reservations.reserve(new ReserveStockCommand(orderId, List.of(
                new ReserveStockCommand.Line(UUID.randomUUID(), scarce, "RACE-SCARCE", 1, true),
                new ReserveStockCommand.Line(UUID.randomUUID(), abundant, "RACE-ABUNDANT", 1, true)), EXPIRY));
        });

        assertThat(outcomes).filteredOn(Attempt::succeeded).hasSize(5);
        assertThat(outcomes).filteredOn(Attempt::shortfall).hasSize(19);
        assertThat(reserved(scarce)).isEqualTo(5);
        // A losing request cannot leave its otherwise-available line held (UC-INV-01 E3).
        assertThat(reserved(abundant)).isEqualTo(5);
    }

    @Test
    void concurrentDuplicateRequestsReturnOneExistingReservation() throws Exception {
        UUID itemId = seedStock("RACE-DUPLICATE", 2);
        UUID orderId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        List<Attempt> outcomes = race(24, () -> reserve(orderId, itemId, "RACE-DUPLICATE", lineId));

        assertThat(outcomes).filteredOn(Attempt::succeeded).hasSize(24);
        assertThat(outcomes).extracting(Attempt::reservationId).doesNotContainNull().hasSize(24)
            .containsOnly(outcomes.getFirst().reservationId());
        assertThat(reserved(itemId)).isEqualTo(1);
    }

    private ReservationSet reserve(UUID orderId, UUID stockItemId, String sku, UUID lineId) {
        return reservations.reserve(new ReserveStockCommand(orderId,
            List.of(new ReserveStockCommand.Line(lineId, stockItemId, sku, 1, true)), EXPIRY));
    }

    private UUID seedStock(String sku, int onHand) {
        UUID stockItemId = UUID.randomUUID();
        transactions.executeWithoutResult(status -> stockItems.saveAll(List.of(StockItem.open(stockItemId, sku, warehouseId, onHand))));
        return stockItemId;
    }

    private int reserved(UUID stockItemId) {
        return jdbc.queryForObject("select quantity_reserved from inventory_l5.inventory_stock_item where id = ?",
            Integer.class, stockItemId);
    }

    private int onHand(UUID stockItemId) {
        return jdbc.queryForObject("select quantity_on_hand from inventory_l5.inventory_stock_item where id = ?",
            Integer.class, stockItemId);
    }

    private static List<Attempt> race(int threads, ReservationAttempt operation) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Attempt>> futures = new ArrayList<>();
            for (int index = 0; index < threads; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        ReservationSet result = operation.reserve();
                        return Attempt.success(result.reservations().getFirst().reservationId());
                    } catch (InsufficientStockException shortfall) {
                        return Attempt.failedShortfall();
                    }
                }));
            }
            ready.await();
            start.countDown();
            List<Attempt> outcomes = new ArrayList<>();
            for (Future<Attempt> future : futures) {
                outcomes.add(future.get());
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface ReservationAttempt {
        ReservationSet reserve();
    }

    private record Attempt(UUID reservationId, boolean succeeded, boolean shortfall) {
        static Attempt success(UUID reservationId) { return new Attempt(reservationId, true, false); }
        static Attempt failedShortfall() { return new Attempt(null, false, true); }
    }
}
