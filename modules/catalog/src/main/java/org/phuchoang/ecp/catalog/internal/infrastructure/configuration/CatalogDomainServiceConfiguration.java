package org.phuchoang.ecp.catalog.internal.infrastructure.configuration;

import org.phuchoang.ecp.catalog.internal.domain.policy.CategoryHierarchyPolicy;
import org.phuchoang.ecp.catalog.internal.domain.repository.CategoryRepository;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;
import org.phuchoang.ecp.catalog.internal.domain.service.CategoryCommandService;
import org.phuchoang.ecp.catalog.internal.domain.service.ProductCommandService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires Catalog's framework-free hierarchy policy into application services. */
@Configuration(proxyBeanMethods = false)
public class CatalogDomainServiceConfiguration {

    @Bean
    CategoryHierarchyPolicy categoryHierarchyPolicy() {
        return new CategoryHierarchyPolicy();
    }

    @Bean
    ProductCommandService productCommandService(ProductRepository products) {
        return new ProductCommandService(products);
    }

    @Bean
    CategoryCommandService categoryCommandService(CategoryRepository categories, ProductRepository products,
            CategoryHierarchyPolicy hierarchyPolicy) {
        return new CategoryCommandService(categories, products, hierarchyPolicy);
    }
}
