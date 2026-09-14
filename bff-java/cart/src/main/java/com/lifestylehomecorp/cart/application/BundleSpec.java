package com.lifestylehomecorp.cart.application;

import java.util.List;

/**
 * A bundle resolved to exactly what the cart needs: the parent product + master variant (added as the
 * bundle-identity line, priced externally at 0) and its child components (added as real line items).
 * A product that is not a bundle resolves to empty {@code components} — the service turns that into a
 * 4xx rather than a partial cart.
 */
public record BundleSpec(String productId, long variantId, String key, List<BundleComponent> components) {

    public boolean isBundle() {
        return components != null && !components.isEmpty();
    }
}
