package org.phuchoang.ecp.inventory.api.reservation;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.commit.CommitStockReservationRequest;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.commit.CommitStockReservationResult;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.commit.CommitStockReservationService;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.release.ReleaseStockReservationRequest;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.release.ReleaseStockReservationResult;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.release.ReleaseStockReservationService;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve.ReserveStockRequest;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve.ReserveStockResult;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve.ReserveStockService;
import org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve.ReservedStock;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryReservationFacadeTest {

    @Test
    void mapsThePublicReservationContractAtTheApiBoundary() {
        ReserveStockService reserve = mock(ReserveStockService.class);
        InventoryReservationFacade facade = facade(reserve);
        UUID orderId = UUID.randomUUID();
        UUID orderLineId = UUID.randomUUID();
        UUID stockItemId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2030-01-01T00:00:00Z");
        ReserveStockCommand command = new ReserveStockCommand(orderId,
            List.of(new ReserveStockCommand.Line(orderLineId, stockItemId, "SKU-1", 2, true)), expiresAt);
        when(reserve.reserve(any())).thenReturn(new ReserveStockResult(orderId,
            List.of(new ReservedStock(reservationId, stockItemId, orderId, orderLineId, 2,
                ReservedStock.Status.HELD, expiresAt, null, null))));

        ReservationSet result = facade.reserve(command);

        assertThat(result.reservations()).containsExactly(new ReservationView(reservationId, stockItemId, orderId,
            orderLineId, 2, ReservationView.Status.HELD, expiresAt, null, null));
        verify(reserve).reserve(new ReserveStockRequest(orderId,
            List.of(new ReserveStockRequest.Line(orderLineId, stockItemId, "SKU-1", 2, true)), expiresAt));
    }

    @Test
    void translatesApplicationShortfallsToTheStablePublicException() {
        ReserveStockService reserve = mock(ReserveStockService.class);
        InventoryReservationFacade facade = facade(reserve);
        UUID orderLineId = UUID.randomUUID();
        when(reserve.reserve(any())).thenThrow(
            new org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve.InsufficientStockException(List.of(
                new org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve.StockShortfall(
                    orderLineId, "SKU-1", 3, 1))));

        ReserveStockCommand command = new ReserveStockCommand(UUID.randomUUID(),
            List.of(new ReserveStockCommand.Line(orderLineId, UUID.randomUUID(), "SKU-1", 3, true)),
            Instant.parse("2030-01-01T00:00:00Z"));

        assertThatThrownBy(() -> facade.reserve(command))
            .isInstanceOf(InsufficientStockException.class)
            .satisfies(exception -> assertThat(((InsufficientStockException) exception).shortfalls())
                .containsExactly(new StockShortfall(orderLineId, "SKU-1", 3, 1)));
    }

    @Test
    void mapsReleaseAndCommitUseCaseBoundariesAtTheApiFacade() {
        ReserveStockService reserve = mock(ReserveStockService.class);
        ReleaseStockReservationService release = mock(ReleaseStockReservationService.class);
        CommitStockReservationService commit = mock(CommitStockReservationService.class);
        InventoryReservationFacade facade = new InventoryReservationFacade(reserve, release, commit);
        UUID stockItemId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID orderLineId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2030-01-01T00:00:00Z");
        ReservationReference reference = new ReservationReference(stockItemId, reservationId);
        when(release.release(any())).thenReturn(new ReleaseStockReservationResult(reservationId, stockItemId, orderId,
            orderLineId, 2, ReleaseStockReservationResult.Status.RELEASED, expiresAt, expiresAt, null,
            ReleaseStockReservationResult.Outcome.RELEASED));
        when(commit.commit(any())).thenReturn(new CommitStockReservationResult(reservationId, stockItemId, orderId,
            orderLineId, 2, CommitStockReservationResult.Status.COMMITTED, expiresAt, expiresAt, null,
            CommitStockReservationResult.Outcome.COMMITTED));
        IdentityActor caller = new IdentityActor(UUID.randomUUID(), Set.of("WAREHOUSE_OPERATOR"));

        ReservationActionResult released = facade.release(reference, false);
        ReservationActionResult committed = facade.commitStockReservation(caller, reference);

        assertThat(released).isEqualTo(new ReservationActionResult(new ReservationView(reservationId, stockItemId,
            orderId, orderLineId, 2, ReservationView.Status.RELEASED, expiresAt, expiresAt, null),
            ReservationActionResult.Outcome.RELEASED));
        assertThat(committed).isEqualTo(new ReservationActionResult(new ReservationView(reservationId, stockItemId,
            orderId, orderLineId, 2, ReservationView.Status.COMMITTED, expiresAt, expiresAt, null),
            ReservationActionResult.Outcome.COMMITTED));
        verify(release).release(new ReleaseStockReservationRequest(stockItemId, reservationId, false));
        verify(commit).commit(new CommitStockReservationRequest(caller, stockItemId, reservationId));
    }

    private static InventoryReservationFacade facade(ReserveStockService reserve) {
        return new InventoryReservationFacade(reserve, mock(ReleaseStockReservationService.class),
            mock(CommitStockReservationService.class));
    }
}
