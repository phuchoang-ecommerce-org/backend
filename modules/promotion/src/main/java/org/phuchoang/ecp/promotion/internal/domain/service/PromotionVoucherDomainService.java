package org.phuchoang.ecp.promotion.internal.domain.service;

import org.jmolecules.ddd.annotation.Service;
import org.phuchoang.ecp.promotion.internal.domain.model.Promotion;
import org.phuchoang.ecp.promotion.internal.domain.model.PromotionVoucher;
import org.phuchoang.ecp.promotion.internal.domain.repository.PromotionRepository;

import java.security.SecureRandom;
import java.util.List;

/** Invokes the Promotion root's voucher-generation operation. */
@Service
public final class PromotionVoucherDomainService {

  private final PromotionRepository promotions;
  private final SecureRandom random;

  public PromotionVoucherDomainService(PromotionRepository promotions, SecureRandom random) {
    this.promotions = promotions;
    this.random = random;
  }

  public List<PromotionVoucher> generate(Promotion.GenerateVouchers request) {
    return Promotion.generateVouchers(request, random, promotions);
  }
}
