package org.phuchoang.ecp.identity.internal.infrastructure.configuration;

import org.phuchoang.ecp.identity.internal.domain.policy.AccessControlPolicy;
import org.phuchoang.ecp.identity.internal.domain.policy.AddressBookPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires Identity's framework-free domain policies into application services. */
@Configuration(proxyBeanMethods = false)
public class IdentityDomainServiceConfiguration {

    @Bean
    AccessControlPolicy accessControlPolicy() {
        return new AccessControlPolicy();
    }

    @Bean
    AddressBookPolicy addressBookPolicy() {
        return new AddressBookPolicy();
    }
}
