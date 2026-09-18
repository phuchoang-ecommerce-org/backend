package org.phuchoang.ecp.messaging.kafka;

/**
 * The topic names, declared once so the catalogue that provisions them and the listeners that
 * subscribe to them can never spell the same stream differently. Bounded contexts that
 * <em>publish</em> keep their own constant next to their outbox writer (a module cannot depend on
 * the composition root); the catalogue is the authority that both must agree with.
 */
public final class KafkaTopics {

    public static final String ORDERING_ORDER = "ecp.ordering.order.v1";
    public static final String PAYMENT_PAYMENT = "ecp.payment.payment.v1";
    public static final String INVENTORY_STOCK_ITEM = "ecp.inventory.stockitem.v1";
    public static final String CATALOG_PRODUCT = "ecp.catalog.product.v1";
    public static final String CATALOG_CATEGORY = "ecp.catalog.category.v1";
    public static final String SHIPPING_SHIPMENT = "ecp.shipping.shipment.v1";
    public static final String PROMOTION_PROMOTION = "ecp.promotion.promotion.v1";
    public static final String REVIEW_REVIEW = "ecp.review.review.v1";
    public static final String IDENTITY_ACCOUNT = "ecp.identity.account.v1";

    private KafkaTopics() {
    }
}
