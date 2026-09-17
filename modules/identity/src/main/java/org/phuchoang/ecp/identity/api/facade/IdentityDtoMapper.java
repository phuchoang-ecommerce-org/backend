package org.phuchoang.ecp.identity.api.facade;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.identity.api.request.AddressWriteRequest;
import org.phuchoang.ecp.identity.api.request.LoginRequest;
import org.phuchoang.ecp.identity.api.request.ProfileUpdateRequest;
import org.phuchoang.ecp.identity.api.request.RegisterAccountRequest;
import org.phuchoang.ecp.identity.api.view.AccountView;
import org.phuchoang.ecp.identity.api.view.AddressView;
import org.phuchoang.ecp.identity.api.view.SessionResponse;
import org.phuchoang.ecp.identity.internal.application.command.model.AddressCommand;
import org.phuchoang.ecp.identity.internal.application.command.model.LoginCommand;
import org.phuchoang.ecp.identity.internal.application.command.model.RegisterAccountCommand;
import org.phuchoang.ecp.identity.internal.application.command.model.UpdateProfileCommand;
import org.phuchoang.ecp.identity.internal.application.mapper.AccountSummary;
import org.phuchoang.ecp.identity.internal.application.mapper.AddressSummary;
import org.phuchoang.ecp.identity.internal.application.mapper.LoginResult;
import org.phuchoang.ecp.sharedkernel.api.address.Address;

/** Maps identity's API records to application commands and application results to API views. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface IdentityDtoMapper {

    RegisterAccountCommand registerAccountCommand(RegisterAccountRequest request);

    LoginCommand loginCommand(LoginRequest request);

    UpdateProfileCommand updateProfileCommand(ProfileUpdateRequest request);

    @Mapping(target = "expiresIn", source = "expiresInSeconds")
    SessionResponse sessionResponse(LoginResult result);

    AccountView accountView(AccountSummary summary);

    AddressView addressView(AddressSummary summary);

    default AddressCommand addressCommand(AddressWriteRequest request) {
        Address address = new Address(request.label(), request.recipientName(), request.line1(), request.line2(),
            request.city(), request.region(), request.postalCode(), request.countryCode(), request.phone());
        return new AddressCommand(address, Boolean.TRUE.equals(request.isDefaultShipping()),
            Boolean.TRUE.equals(request.isDefaultBilling()));
    }
}
