package org.phuchoang.ecp.promotion.internal.infrastructure.configuration;

import org.phuchoang.ecp.promotion.internal.domain.repository.PromotionRepository;
import org.phuchoang.ecp.promotion.internal.domain.service.PromotionCreationDomainService;
import org.phuchoang.ecp.promotion.internal.domain.service.PromotionRedemptionDomainService;
import org.phuchoang.ecp.promotion.internal.domain.service.PromotionVoucherDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;

/** Registers Promotion's framework-free domain services as Spring beans. */
@Configuration(proxyBeanMethods = false)
public class PromotionDomainServiceConfiguration {

    @Bean
    PromotionCreationDomainService promotionCreationDomainService(PromotionRepository promotions) {
        return new PromotionCreationDomainService(promotions);
    }

    @Bean
    PromotionVoucherDomainService promotionVoucherDomainService(PromotionRepository promotions) {
        return new PromotionVoucherDomainService(promotions, new SecureRandom());
    }

    @Bean
    PromotionRedemptionDomainService promotionRedemptionDomainService(PromotionRepository promotions) {
        return new PromotionRedemptionDomainService(promotions);
    }
}
