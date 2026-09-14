<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Product Backlog — Enterprise Commerce Platform (ECP)

**Document type:** Derived lane backlog · **Audience:** Backend Engineering, Product Management
**Canonical source:** [PM product backlog](../product-backlog.md)

---

## How to Use This View

This is the backend-actionable view of the canonical integrated backlog. It contains every story with non-zero BE points, preserves its sprint and contract surface, and records the other lane only as a delivery milestone. Zero-point stories are intentionally omitted unless they are named in a sprint dependency.

**Scheduled work: 423 story points + 228 enabler points = 651 points.** Estimates, priority, and schedule belong to the [canonical backlog](../product-backlog.md); update that source, then regenerate this view.

## Shared Rules

- One story is not Done until both slices and its Contract Sync have passed; see the [canonical Definition of Done](../definition-of-done.md#5-definition-of-done--the-story).
- The OpenAPI contract is normative. Amendments follow the [shared integration protocol](../integration-plan.md#5-amending-the-contract).
- The other-lane milestone is context, not a second task list. Full cross-lane scope remains in the [canonical backlog](../product-backlog.md).

## User Stories

### 4.1 Customer & Identity — 10 stories · BE 39 · FE 30 — Backend 39 pts

| ID | Story | P | BE pts | Backend sprint | Other-lane milestone | Contract surface |
|---|---|---|---:|---|---|---|
| `US-CUS-01` | Register Customer Account | Must | 5 | S03 | S03 | `registerAccount`<br>/register |
| `US-CUS-02` | Verify Email Address | Must | 3 | S03 | S03 | `verifyEmailAddress · resendEmailVerification`<br>/verify-email |
| `US-CUS-03` | Log In | Must | 5 | S03 | S03 | `logIn`<br>/sign-in |
| `US-CUS-04` | Log Out | Must | 2 | S03 | S03 | `logOut`<br>account menu action |
| `US-CUS-05` | Refresh Authenticated Session | Must | 5 | S04 | S04 | `renewSession`<br>internal — serialised refresh |
| `US-CUS-06` | Change Password | Must | 3 | S05 | S05 | `changeOwnPassword`<br>/account/security |
| `US-CUS-07` | Reset Forgotten Password | Must | 5 | S05 | S05 | `requestPasswordReset · completePasswordReset`<br>/forgot-password · /reset-password |
| `US-CUS-08` | Manage Profile | Must | 3 | S05 | S05 | `getOwnAccount · updateOwnProfile`<br>/account/profile |
| `US-CUS-09` | Manage Shipping Addresses | Must | 5 | S05 | S05 | `listOwnAddresses · addOwnAddress · replaceOwnAddress · removeOwnAddress`<br>/account/addresses |
| `US-CUS-10` | View Purchase History | Must | 3 | S05 | S05 | `listOrders`<br>/account/orders |

### 4.2 Product Catalog & Category — 5 stories · BE 20 · FE 24 — Backend 20 pts

| ID | Story | P | BE pts | Backend sprint | Other-lane milestone | Contract surface |
|---|---|---|---:|---|---|---|
| `US-CAT-01` | Browse Category Tree | Must | 5 | S06 | S06 | `listCategories · getCategory`<br>/c/[...slug] |
| `US-CAT-02` | Browse Category Product Listing | Must | 5 | S06 | S06 | `listCategoryProducts`<br>/c/[...slug] |
| `US-CAT-03` | View Product Details | Must | 5 | S07 | S07 | `getProduct · listProductVariants · getProductRatingSummary`<br>/p/[productId] |
| `US-CAT-04` | Select Product Variant | Must | 3 | S06 | S07 | `listProductVariants · getProductVariant`<br>/p/[productId] |
| `US-CAT-05` | View Featured Categories | Should | 2 | S30 | S30 | `listFeaturedCategories`<br>/ |

### 4.3 Search & Recommendation — 7 stories · BE 34 · FE 27 — Backend 34 pts

| ID | Story | P | BE pts | Backend sprint | Other-lane milestone | Contract surface |
|---|---|---|---:|---|---|---|
| `US-SCH-01` | Search Products by Keyword | Must | 8 | S10 | S10 | `searchProducts`<br>/search |
| `US-SCH-02` | Search Suggestions and Auto-complete | Should | 5 | S30 | S30 | `getSearchSuggestions`<br>/search — typeahead |
| `US-SCH-03` | Filter and Sort Search Results | Must | 5 | S10 | S10 | `searchProducts (facets)`<br>/search |
| `US-SCH-04` | View Popular and Recent Keywords | Could | 3 | S30 | S30 | `listPopularKeywords · listOwnRecentKeywords · removeOwnRecentKeyword · clearOwnRecentKeywords`<br>/search |
| `US-SCH-05` | View Related and Frequently-Bought-Together | Should | 5 | S30 | S30 | `listRelatedProducts · listFrequentlyBoughtTogetherForProduct · listFrequentlyBoughtTogetherForCart`<br>/p/[productId] · /cart |
| `US-SCH-06` | View Trending Products and New Arrivals | Could | 3 | S30 | S30 | `listTrendingProducts · listNewArrivals`<br>/ |
| `US-SCH-07` | Receive Personalised Recommendations | Could | 5 | S31 | S31 | `listPersonalisedRecommendations · setOwnPersonalisationPreference`<br>/ — personalised rail |

### 4.4 Inventory — 5 stories · BE 28 · FE 10 — Backend 28 pts

| ID | Story | P | BE pts | Backend sprint | Other-lane milestone | Contract surface |
|---|---|---|---:|---|---|---|
| `US-INV-01` | Reserve Stock for an Order | Must | 8 | S11 | — | `StockReservationPort (internal)`<br>none — internal |
| `US-INV-02` | Release Reserved Stock | Must | 5 | S11 | — | `StockReservationPort (internal)`<br>none — internal |
| `US-INV-03` | Commit Reserved Stock on Fulfilment | Must | 5 | S11 | S11 | `commitStockReservation`<br>/admin/inventory/[stockItemId] |
| `US-INV-04` | Adjust Inventory | Must | 5 | S12 | S12 | `adjustStock · listStockAdjustments`<br>/admin/inventory/[stockItemId] |
| `US-INV-05` | View Inventory Levels | Must | 5 | S12 | S12 | `listStockItems · getStockItem · listWarehouses`<br>/admin/inventory |

### 4.5 Cart & Wishlist — 8 stories · BE 31 · FE 23 — Backend 31 pts

| ID | Story | P | BE pts | Backend sprint | Other-lane milestone | Contract surface |
|---|---|---|---:|---|---|---|
| `US-CRT-01` | Add Item to Cart | Must | 5 | S13 | S13 | `addCartLine`<br>/cart · /p/[productId] |
| `US-CRT-02` | Update Cart Item Quantity | Must | 3 | S13 | S13 | `updateCartLineQuantity`<br>/cart |
| `US-CRT-03` | Remove Item from Cart | Must | 2 | S13 | S13 | `removeCartLine`<br>/cart |
| `US-CRT-04` | View Cart | Must | 5 | S13 | S13 | `getCurrentCart · getCart`<br>/cart |
| `US-CRT-05` | Merge Guest Cart on Login | Must | 5 | S14 | S14 | `mergeGuestCart`<br>sign-in path, server-side |
| `US-CRT-06` | Expire Inactive Cart | Must | 3 | S14 | — | `scheduler (internal)`<br>none — scheduler |
| `US-CRT-07` | Manage Wishlist | Should | 5 | S31 | S31 | `getOwnWishlist · addWishlistItem · removeWishlistItem`<br>/wishlist |
| `US-CRT-08` | Move Wishlist Item to Cart | Should | 3 | S31 | S31 | `moveWishlistItemsToCart`<br>/wishlist |

### 4.6 Checkout & Order — 10 stories · BE 55 · FE 39 — Backend 55 pts

| ID | Story | P | BE pts | Backend sprint | Other-lane milestone | Contract surface |
|---|---|---|---:|---|---|---|
| `US-ORD-01` | Initiate Checkout | Must | 5 | S17 | S14 | `initiateCheckout · getCurrentCheckout`<br>/checkout |
| `US-ORD-02` | Provide Shipping and Billing Information | Must | 5 | S17 | S14 | `setCheckoutShippingAddress · setCheckoutBillingInformation · selectCheckoutShippingOption`<br>/checkout/shipping · /checkout/payment |
| `US-ORD-03` | Apply Voucher at Checkout | Must | 3 | S17 | S15 | `applyCheckoutVoucher · removeCheckoutVoucher`<br>/checkout/review |
| `US-ORD-04` | Review Order Summary | Must | 5 | S17 | S15 | `getOrderSummary`<br>/checkout/review |
| `US-ORD-05` | Place Order | Must | 13 | S18 | S17 | `placeOrder`<br>/checkout/review · /checkout/confirmation/[orderId] |
| `US-ORD-06` | View Order Details | Must | 3 | S19 | S17 | `getOrder · listOrderLines`<br>/account/orders/[orderId] |
| `US-ORD-07` | Track Order | Must | 3 | S19 | S18 | `trackOrder`<br>/account/orders/[orderId]/tracking |
| `US-ORD-08` | Cancel Order | Must | 5 | S19 | S18 | `cancelOrder`<br>/account/orders/[orderId] |
| `US-ORD-09` | Request Return | Should | 5 | S31 | S31 | `requestOrderReturn · getOrderReturnRequest · resolveOrderReturn`<br>/account/orders/[orderId]/return |
| `US-ORD-10` | Advance Order Status | Must | 8 | S19 | S18 | `advanceOrderStatus · advanceOrderStatusesInBulk · setOrderInvestigationFlag`<br>/admin/orders/[orderId] |

### 4.7 Payment — 6 stories · BE 34 · FE 19 — Backend 34 pts

| ID | Story | P | BE pts | Backend sprint | Other-lane milestone | Contract surface |
|---|---|---|---:|---|---|---|
| `US-PAY-01` | Select Payment Method | Must | 3 | S21 | S20 | `listEligiblePaymentMethods · selectCheckoutPaymentMethod`<br>/checkout/payment |
| `US-PAY-02` | Authorise Online Payment | Must | 8 | S21 | S21 | `initiatePayment · getOrderPayment`<br>/checkout/payment/processing |
| `US-PAY-03` | Handle Payment Gateway Result | Must | 8 | S21 | S21 | `receivePaymentProviderNotification`<br>/api/auth/* — provider return handler |
| `US-PAY-04` | Settle Cash On Delivery Payment | Must | 5 | S22 | S22 | `settleCashOnDelivery`<br>/admin/payments/[paymentId] |
| `US-PAY-05` | Retry Failed Payment | Must | 5 | S22 | S22 | `retryPayment`<br>/checkout/payment/processing |
| `US-PAY-06` | Process Refund | Must | 5 | S22 | S22 | `refundPayment · listPaymentRefunds · listPaymentAttempts · listUnmatchedPayments`<br>/admin/payments/[paymentId] |

### 4.8 Shipping — 6 stories · BE 24 · FE 14 — Backend 24 pts

| ID | Story | P | BE pts | Backend sprint | Other-lane milestone | Contract surface |
|---|---|---|---:|---|---|---|
| `US-SHP-01` | Calculate Shipping Fee | Must | 5 | S20 | S19 | `getShippingQuotes`<br>/checkout/shipping |
| `US-SHP-02` | Estimate Delivery Date | Should | 3 | S31 | S31 | `getShippingQuotes (estimate)`<br>/checkout/shipping |
| `US-SHP-03` | Create Shipment | Must | 5 | S20 | S20 | `createShipment · listShipments`<br>/admin/shipments |
| `US-SHP-04` | Record Carrier Tracking Update | Must | 5 | S20 | — | `receiveCarrierEvent`<br>none — system actor |
| `US-SHP-05` | View Shipment Tracking | Must | 3 | S20 | S19 | `getShipment · listShipmentTrackingEvents`<br>/account/orders/[orderId]/tracking |
| `US-SHP-06` | Confirm Delivery | Must | 3 | S20 | S20 | `confirmShipmentDelivery`<br>/admin/shipments/[shipmentId] |

### 4.9 Promotion — 5 stories · BE 29 · FE 15 — Backend 29 pts

| ID | Story | P | BE pts | Backend sprint | Other-lane milestone | Contract surface |
|---|---|---|---:|---|---|---|
| `US-PRM-01` | Create Promotion | Must | 8 | S15 | S16 | `createPromotion · listPromotions · generatePromotionVouchers`<br>/admin/promotions |
| `US-PRM-02` | Validate Voucher Code | Must | 5 | S15 | S15 | `validateVoucher`<br>/checkout/review |
| `US-PRM-03` | Apply Promotion to Order | Must | 8 | S15 | S15 | `PromotionRedemptionPort · listPromotionRedemptions`<br>/checkout/review |
| `US-PRM-04` | Launch Flash Sale | Must | 5 | S16 | S16 | `setPromotionStatus`<br>/admin/promotions/[promotionId] |
| `US-PRM-05` | Deactivate or Expire Promotion | Must | 3 | S16 | S16 | `updatePromotion · setPromotionStatus`<br>/admin/promotions/[promotionId] |

### 4.10 Review — 5 stories · BE 20 · FE 18 — Backend 20 pts

| ID | Story | P | BE pts | Backend sprint | Other-lane milestone | Contract surface |
|---|---|---|---:|---|---|---|
| `US-REV-01` | Submit Product Review | Must | 5 | S24 | S21 | `submitProductReview · addReviewImage`<br>/p/[productId] · /account/orders/[orderId] |
| `US-REV-02` | Edit Own Review | Should | 3 | S32 | S32 | `editOwnReview`<br>/account/reviews |
| `US-REV-03` | Delete Own Review | Should | 2 | S32 | S32 | `deleteOwnReview · removeReviewImage`<br>/account/reviews |
| `US-REV-04` | View Product Reviews | Must | 5 | S24 | S21 | `listProductReviews · getProductRatingSummary · reportReview`<br>/p/[productId] |
| `US-REV-05` | Moderate Review | Should | 5 | S32 | S32 | `moderateReview · listReviews · getReview`<br>/admin/reviews |

### 4.11 Notification — 4 stories · BE 19 · FE 8 — Backend 19 pts

| ID | Story | P | BE pts | Backend sprint | Other-lane milestone | Contract surface |
|---|---|---|---:|---|---|---|
| `US-NTF-01` | Deliver Email Notification | Must | 8 | S23 | — | `listNotificationDeliveries`<br>/admin/notifications |
| `US-NTF-02` | Deliver In-App Notification | Must | 5 | S23 | — | `internal consumer`<br>none — Kafka consumer |
| `US-NTF-03` | View In-App Notifications | Must | 3 | S23 | S21 | `listOwnNotifications · setNotificationReadState · dismissNotification · markNotificationsRead`<br>/account/notifications |
| `US-NTF-04` | Manage Notification Preferences | Should | 3 | S32 | S32 | `getOwnNotificationPreferences · setOwnNotificationPreferences · unsubscribeFromPromotionalNotifications`<br>/account/preferences · /unsubscribe |

### 4.12 Administration — 6 stories · BE 31 · FE 29 — Backend 31 pts

| ID | Story | P | BE pts | Backend sprint | Other-lane milestone | Contract surface |
|---|---|---|---:|---|---|---|
| `US-ADM-01` | Manage Products | Must | 8 | S09 | S08 | `createProduct · updateProduct · deleteProduct · setProductPublication · addProductVariant · changeVariantPrice · addProductImage · amendProductsInBulk`<br>/admin/products |
| `US-ADM-02` | Manage Categories | Must | 5 | S09 | S08 | `createCategory · updateCategory · deleteCategory`<br>/admin/categories |
| `US-ADM-03` | Manage Customer Accounts | Must | 5 | S25 | S22 | `searchAccounts · getAccount · correctAccountProfile · setAccountStatus · closeAccount · endAccountSessions`<br>/admin/customers |
| `US-ADM-04` | Manage Orders | Must | 5 | S25 | S22 | `listOrders (scoped) · advanceOrderStatusesInBulk`<br>/admin/orders |
| `US-ADM-05` | Manage Inventory Adjustments | Must | 3 | S12 | S09 | `listStockAdjustments`<br>/admin/inventory |
| `US-ADM-06` | Manage User Roles | Must | 5 | S25 | S22 | `listRoles · listAccountRoles · grantAccountRole · revokeAccountRole`<br>/admin/roles |

### 4.13 Reporting & Analytics — 6 stories · BE 33 · FE 24 — Backend 33 pts

| ID | Story | P | BE pts | Backend sprint | Other-lane milestone | Contract surface |
|---|---|---|---:|---|---|---|
| `US-RPT-01` | View Revenue Report | Must | 8 | S26 | S23 | `getRevenueReport`<br>/admin/reports/revenue · /admin |
| `US-RPT-02` | View Product Performance Report | Must | 5 | S26 | S23 | `getProductPerformanceReport`<br>/admin/reports/products |
| `US-RPT-03` | View Customer Report | Should | 5 | S32 | S32 | `getCustomerReport`<br>/admin/reports/customers |
| `US-RPT-04` | View Inventory Report | Must | 5 | S27 | S24 | `getInventoryReport`<br>/admin/reports/inventory · /admin |
| `US-RPT-05` | View Order and Conversion Statistics | Must | 5 | S26 | S23 | `getOrderStatisticsReport`<br>/admin/reports/orders · /admin |
| `US-RPT-06` | Export Report | Could | 5 | S32 | S32 | `requestReportExport · getReportExport · downloadReportExport`<br>/admin/reports/exports |

### 4.14 Audit & Access Control — 4 stories · BE 26 · FE 9 — Backend 26 pts

| ID | Story | P | BE pts | Backend sprint | Other-lane milestone | Contract surface |
|---|---|---|---:|---|---|---|
| `US-AUD-01` | Record Audit Entry | Must | 8 | S12 | — | `internal — every command path`<br>none — cross-cutting |
| `US-AUD-02` | Search Audit Trail | Must | 5 | S28 | S25 | `searchAuditTrail · getAuditEntry`<br>/admin/audit |
| `US-AUD-03` | Authorise Request via RBAC | Must | 8 | S04 | S04 | `every operation — AuthorizationService`<br>cross-cutting — 403/404 rendering |
| `US-AUD-04` | Enforce API Rate Limit | Must | 5 | S04 | S04 | `every operation — Redis limiter`<br>cross-cutting — 429 rendering |

## Enabler Epics

Enablers are owned entirely by this lane and remain scheduled work, never background work.

| Epic | ID | Item | BE pts | Sprint |
|---|---|---|---:|---|
| `EN-BUILD` | `EN-BUILD-1` | Gradle multi-project: 14 subprojects, version catalog, `app` composition root, `bootJar` | 14 | S00 |
| `EN-GATE` | `EN-GATE-1` | ArchUnit + JMolecules + `ApplicationModules.verify()`; `package-info.java` allow-lists; planted-violation demo | 13 | S01 |
| `EN-DATA` | `EN-DATA-1` | PostgreSQL + Kafka in `compose.yaml`; Testcontainers PostgreSQL + JDBC driver | 7 | S01 |
| `EN-DATA` | `EN-DATA-2` | Flyway baseline, per-module table-prefix conventions, migration map | 8 | S02 |
| `EN-DATA` | `EN-DATA-3` | Catalog schema, indexes, and cursor-pagination query design | 8 | S07 |
| `EN-DATA` | `EN-DATA-4` | L5 persistence & concurrency suite scaffolding (schema-per-test-class) | 5 | S14 |
| `EN-DATA` | `EN-DATA-5` | MongoDB reporting read models; projection lag measurement against NFR-PERF-06 | 8 | S27 |
| `EN-WIRE` | `EN-WIRE-1` | Problem+JSON `@RestControllerAdvice`, error-code registry enums, pagination envelope, correlation-id filter | 12 | S02 |
| `EN-WIRE` | `EN-WIRE-2` | Redis two-instance topology; cache-aside and rate-limiter infrastructure | 5 | S03 |
| `EN-WIRE` | `EN-WIRE-3` | Catalog read cache-aside + invalidation keys | 8 | S06 |
| `EN-WIRE` | `EN-WIRE-4` | Guest-cart cookie handling and cart identity resolution | 5 | S13 |
| `EN-EVENT` | `EN-EVENT-1` | Transactional outbox table + polling relay + event envelope + topic catalogue | 21 | S08 |
| `EN-EVENT` | `EN-EVENT-2` | Catalog events published; consumer idempotency and ordering guards | 8 | S09 |
| `EN-EVENT` | `EN-EVENT-3` | Elasticsearch search read model, projected from catalog events | 8 | S10 |
| `EN-EVENT` | `EN-EVENT-4` | Consumer obligations: retry, dead-lettering, replay from outbox | 8 | S14 |
| `EN-EVENT` | `EN-EVENT-5` | Order lifecycle events on the outbox; ordering topic partitioning | 5 | S18 |
| `EN-EVENT` | `EN-EVENT-6` | Review rating-summary projection; MongoDB read-model idempotency tests | 8 | S24 |
| `EN-OBS` | `EN-OBS-1` | Structured JSON logging to stdout; management port; liveness/readiness | 2 | S04 |
| `EN-OBS` | `EN-OBS-2` | Micrometer meters named in Deployment §8 | 5 | S07 |
| `EN-OBS` | `EN-OBS-3` | Correlation id traced REST → outbox → Kafka → projection | 5 | S22 |
| `EN-OBS` | `EN-OBS-4` | NFR-AVAIL-02 dependency-failure harness (stop ES/Mongo, assert purchase path) | 5 | S29 |
| `EN-CONTRACT` | `EN-CONTRACT-1` | Contract test spec→code across delivered operations | 13 | S16 |
| `EN-CONTRACT` | `EN-CONTRACT-2` | Contract test code→spec; undocumented endpoint fails the build | 5 | S25 |
| `EN-CONTRACT` | `EN-CONTRACT-3` | Permission-matrix test generated from spec × matrix, all 155 operations | 13 | S28 |
| `EN-CI` | `EN-CI-1` | CI stages 1–4 of Testing Strategy §9 | 8 | S27 |
| `EN-CI` | `EN-CI-2` | CI stages 5–7; security, dependency and image scanning | 8 | S29 |
| `EN-BENCH` | `EN-BENCH-1` | `bench/smoke.js` — k6 scenarios S1–S5 reconciled against `openapi.yaml` | 5 | S23 |
| `EN-BENCH` | `EN-BENCH-2` | L6 event-delivery suite; broker killed mid-relay; read-model rebuild drill | 8 | S29 |
