package org.phuchoang.ecp.identity.api.facade;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.identity.api.request.AddressWriteRequest;
import org.phuchoang.ecp.identity.api.request.ChangePasswordRequest;
import org.phuchoang.ecp.identity.api.request.LoginRequest;
import org.phuchoang.ecp.identity.api.request.ProfileUpdateRequest;
import org.phuchoang.ecp.identity.api.request.RegisterAccountRequest;
import org.phuchoang.ecp.identity.api.view.AccountView;
import org.phuchoang.ecp.identity.api.view.AddressView;
import org.phuchoang.ecp.identity.api.view.AddressPageView;
import org.phuchoang.ecp.identity.api.view.SessionResponse;
import org.phuchoang.ecp.identity.internal.application.address.AddressCommand;
import org.phuchoang.ecp.identity.internal.application.address.AddressPageResult;
import org.phuchoang.ecp.identity.internal.application.address.AddressSummary;
import org.phuchoang.ecp.identity.internal.application.authentication.LoginCommand;
import org.phuchoang.ecp.identity.internal.application.authentication.LoginResult;
import org.phuchoang.ecp.identity.internal.application.password.ChangePasswordCommand;
import org.phuchoang.ecp.identity.internal.application.profile.AccountSummary;
import org.phuchoang.ecp.identity.internal.application.profile.UpdateProfileCommand;
import org.phuchoang.ecp.identity.internal.application.registration.RegisterAccountCommand;
import org.phuchoang.ecp.identity.internal.domain.model.Address;

import java.util.List;

/** Maps identity's API records to application commands and application results to API views. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface IdentityDtoMapper {

    RegisterAccountCommand registerAccountCommand(RegisterAccountRequest request);

    LoginCommand loginCommand(LoginRequest request);

    UpdateProfileCommand updateProfileCommand(ProfileUpdateRequest request);

    @Mapping(target = "endOtherSessions", source = "endOtherSessions", defaultValue = "true")
    ChangePasswordCommand changePasswordCommand(ChangePasswordRequest request);

    @Mapping(target = "expiresIn", source = "expiresInSeconds")
    @Mapping(target = "cartMergeNotices", ignore = true)
    SessionResponse sessionResponse(LoginResult result);

    AccountView accountView(AccountSummary summary);

    AddressView addressView(AddressSummary summary);

    AddressPageView addressPageView(AddressPageResult result);

    default List<AddressView> addressViews(List<AddressSummary> summaries) {
        return summaries.stream().map(this::addressView).toList();
    }

    @Mapping(target = "address", source = "request")
    @Mapping(target = "isDefaultShipping", source = "isDefaultShipping", defaultValue = "false")
    @Mapping(target = "isDefaultBilling", source = "isDefaultBilling", defaultValue = "false")
    AddressCommand addressCommand(AddressWriteRequest request);

    Address address(AddressWriteRequest request);

}
