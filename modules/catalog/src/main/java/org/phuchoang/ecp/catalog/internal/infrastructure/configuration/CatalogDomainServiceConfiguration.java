package org.phuchoang.ecp.catalog.internal.infrastructure.configuration;

import org.phuchoang.ecp.catalog.internal.domain.policy.CategoryHierarchyPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires Catalog's framework-free hierarchy policy into application services. */
@Configuration(proxyBeanMethods = false)
public class CatalogDomainServiceConfiguration {

    @Bean
    CategoryHierarchyPolicy categoryHierarchyPolicy() {
        return new CategoryHierarchyPolicy();
    }
}
