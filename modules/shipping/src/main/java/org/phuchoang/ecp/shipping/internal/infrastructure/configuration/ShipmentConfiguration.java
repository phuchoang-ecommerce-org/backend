package org.phuchoang.ecp.shipping.internal.infrastructure.configuration;

import org.phuchoang.ecp.shipping.internal.domain.repository.ShipmentRepository;
import org.phuchoang.ecp.shipping.internal.domain.service.ShipmentDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ShipmentConfiguration {
  @Bean
  ShipmentDomainService shipmentDomainService(ShipmentRepository shipments) {
    return new ShipmentDomainService(shipments);
  }
}
