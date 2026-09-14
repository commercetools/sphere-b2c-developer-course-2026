/**
 * The shopper's identity for the course BFF: {@link com.lifestylehomecorp.platform.session.ShopperSession}
 * holds the anonymous id, the active cart id and — from Session 5 — the signed-in customer and their
 * customer group. Domain modules read it instead of taking identity from the request (the S5 security
 * boundary: identity lives in the BFF session, never in a {@code customerId} parameter).
 */
package com.lifestylehomecorp.platform.session;
