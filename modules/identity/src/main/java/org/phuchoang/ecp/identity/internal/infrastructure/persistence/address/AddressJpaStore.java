package org.phuchoang.ecp.identity.internal.infrastructure.persistence.address;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Account-scoped address persistence with the descending keyset order used by cursor pagination. */
interface AddressJpaStore extends JpaRepository<AddressEntity, UUID> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update AddressEntity a set a.defaultShipping = false "
        + "where a.accountId = :accountId and a.defaultShipping = true")
    int clearDefaultShipping(@Param("accountId") UUID accountId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update AddressEntity a set a.defaultBilling = false "
        + "where a.accountId = :accountId and a.defaultBilling = true")
    int clearDefaultBilling(@Param("accountId") UUID accountId);

    boolean existsByAccountId(UUID accountId);

    @Query("select a from AddressEntity a where a.accountId = :accountId "
        + "order by a.createdAt desc, a.id desc")
    List<AddressEntity> findFirstPage(@Param("accountId") UUID accountId, Pageable pageable);

    @Query("select a from AddressEntity a where a.accountId = :accountId "
        + "and (a.createdAt < :afterCreatedAt or (a.createdAt = :afterCreatedAt and a.id < :afterId)) "
        + "order by a.createdAt desc, a.id desc")
    List<AddressEntity> findNextPage(@Param("accountId") UUID accountId, @Param("afterCreatedAt") Instant afterCreatedAt,
        @Param("afterId") UUID afterId, Pageable pageable);
}
