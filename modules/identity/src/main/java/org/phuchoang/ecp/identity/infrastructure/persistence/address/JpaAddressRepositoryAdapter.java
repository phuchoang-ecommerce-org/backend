package org.phuchoang.ecp.identity.infrastructure.persistence.address;

import org.phuchoang.ecp.identity.application.port.AddressRepository;
import org.phuchoang.ecp.identity.domain.CustomerAddress;
import org.phuchoang.ecp.sharedkernel.api.Address;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapts {@link CustomerAddress} to {@code identity_address}. */
@Repository
class JpaAddressRepositoryAdapter implements AddressRepository {

    private final AddressJpaRepository addressJpaRepository;

    JpaAddressRepositoryAdapter(AddressJpaRepository addressJpaRepository) {
        this.addressJpaRepository = addressJpaRepository;
    }

    @Override
    public CustomerAddress save(CustomerAddress address) {
        Instant createdAt = addressJpaRepository.findById(address.id())
            .map(AddressEntity::getCreatedAt)
            .orElseGet(Instant::now);
        Address a = address.address();
        AddressEntity entity = new AddressEntity(address.id(), address.accountId(), a.label(), a.recipientName(),
            a.line1(), a.line2(), a.city(), a.region(), a.postalCode(), a.countryCode(), a.phone(),
            address.defaultShipping(), address.defaultBilling(), createdAt);
        return toDomain(addressJpaRepository.save(entity));
    }

    @Override
    public Optional<CustomerAddress> findById(UUID id) {
        return addressJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<CustomerAddress> findByAccountId(UUID accountId, Cursor after, int size) {
        List<AddressEntity> entities = after == null
            ? addressJpaRepository.findFirstPage(accountId, PageRequest.of(0, size))
            : addressJpaRepository.findNextPage(accountId, after.createdAt(), after.id(), PageRequest.of(0, size));
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public Cursor cursorOf(CustomerAddress address) {
        return addressJpaRepository.findById(address.id())
            .map(entity -> new Cursor(entity.getCreatedAt(), entity.getId()))
            .orElseThrow();
    }

    @Override
    public List<CustomerAddress> findAllByAccountId(UUID accountId) {
        return addressJpaRepository.findAllByAccountId(accountId).stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        addressJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsAnyForAccount(UUID accountId) {
        return addressJpaRepository.existsByAccountId(accountId);
    }

    private CustomerAddress toDomain(AddressEntity entity) {
        Address address = new Address(entity.getLabel(), entity.getRecipientName(), entity.getLine1(),
            entity.getLine2(), entity.getCity(), entity.getRegion(), entity.getPostalCode(), entity.getCountryCode(),
            entity.getPhone());
        return CustomerAddress.reconstitute(entity.getId(), entity.getAccountId(), address,
            entity.isDefaultShipping(), entity.isDefaultBilling());
    }
}
