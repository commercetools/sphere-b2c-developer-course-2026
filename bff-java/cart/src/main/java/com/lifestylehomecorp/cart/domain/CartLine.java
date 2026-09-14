package com.lifestylehomecorp.cart.domain;

/**
 * A single cart line, SDK-free. A bundle is a group of lines linked by a client-minted bundle id:
 * the parent carries {@code bundleId} (its identity), each child carries {@code parentKey} equal to
 * that id (4.3) — the storefront groups children under the parent whose {@code bundleId} matches.
 * {@code recurring} marks a subscription line (4.4); the channel/inventory fields carry the per-line
 * fulfilment choices (4.5 / 4.6).
 */
public record CartLine(
        String lineItemId,
        String sku,
        String name,
        long quantity,
        Money unitPrice,
        Money lineTotal,
        Money lineSavings,
        String bundleId,
        String parentKey,
        boolean recurring,
        /** e.g. "Every month" — the recurring line's frequency (null when not recurring). */
        String recurrenceLabel,
        /** "Fixed" (price locked at subscribe time) or "Dynamic" (re-priced each cycle); null when not recurring. */
        String recurrencePriceMode,
        String distributionChannelId,
        String supplyChannelId,
        String inventoryMode) {
}
