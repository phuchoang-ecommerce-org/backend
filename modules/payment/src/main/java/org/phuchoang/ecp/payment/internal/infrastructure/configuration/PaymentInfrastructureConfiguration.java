package org.phuchoang.ecp.payment.internal.infrastructure.configuration;

import org.phuchoang.ecp.payment.internal.domain.repository.PaymentRepository;
import org.phuchoang.ecp.payment.internal.domain.service.PaymentDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PaymentInfrastructureConfiguration {
  @Bean
  PaymentDomainService paymentDomainService(PaymentRepository repository) {
    return new PaymentDomainService(repository);
  }
}
