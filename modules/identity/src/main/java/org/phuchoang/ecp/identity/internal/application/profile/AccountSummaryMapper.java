package org.phuchoang.ecp.identity.internal.application.profile;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.identity.internal.domain.model.Account;

/** Maps the Account aggregate's read-only state into the application-owned profile projection. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AccountSummaryMapper {

    @Mapping(target = "id", expression = "java(source.id().toString())")
    @Mapping(target = "email", expression = "java(source.email().value())")
    @Mapping(target = "displayName", expression = "java(source.displayName())")
    @Mapping(target = "status", expression = "java(source.status().name())")
    @Mapping(target = "verificationStatus", expression = "java(source.verificationStatus().name())")
    @Mapping(target = "pendingEmail", expression = "java(source.pendingEmail() == null ? null : source.pendingEmail().value())")
    @Mapping(target = "roles", expression = "java(source.roles().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()))")
    @Mapping(target = "verifiedAt", expression = "java(source.verifiedAt())")
    @Mapping(target = "lastLoginAt", expression = "java(source.lastLoginAt())")
    @Mapping(target = "createdAt", expression = "java(source.createdAt())")
    AccountSummary toSummary(Account source);
}
