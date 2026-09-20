package org.phuchoang.ecp.identity.internal.infrastructure.persistence.address;

import org.phuchoang.ecp.identity.internal.application.port.AddressStore;
import org.phuchoang.ecp.identity.internal.domain.model.CustomerAddress;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapts {@link CustomerAddress} to {@code identity_address}. */
@Repository
class JpaAddressStoreAdapter implements AddressStore {

    private final AddressJpaStore addressJpaRepository;
    private final AddressJpaMapper mapper;

    JpaAddressStoreAdapter(AddressJpaStore addressJpaRepository, AddressJpaMapper mapper) {
        this.addressJpaRepository = addressJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CustomerAddress save(CustomerAddress address) {
        Instant createdAt = addressJpaRepository.findById(address.id())
            .map(AddressEntity::getCreatedAt)
            .orElseGet(Instant::now);
        return mapper.toDomain(addressJpaRepository.save(mapper.toEntity(address, createdAt)));
    }

    @Override
    public Optional<CustomerAddress> findById(UUID id) {
        return addressJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<CustomerAddress> findByAccountId(UUID accountId, Cursor after, int size) {
        List<AddressEntity> entities = after == null
            ? addressJpaRepository.findFirstPage(accountId, PageRequest.of(0, size))
            : addressJpaRepository.findNextPage(accountId, after.createdAt(), after.id(), PageRequest.of(0, size));
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public Cursor cursorOf(CustomerAddress address) {
        return addressJpaRepository.findById(address.id())
            .map(entity -> new Cursor(entity.getCreatedAt(), entity.getId()))
            .orElseThrow();
    }

    @Override
    public void clearDefaultShipping(UUID accountId) {
        addressJpaRepository.clearDefaultShipping(accountId);
    }

    @Override
    public void clearDefaultBilling(UUID accountId) {
        addressJpaRepository.clearDefaultBilling(accountId);
    }

    @Override
    public void delete(UUID id) {
        addressJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsAnyForAccount(UUID accountId) {
        return addressJpaRepository.existsByAccountId(accountId);
    }

}
