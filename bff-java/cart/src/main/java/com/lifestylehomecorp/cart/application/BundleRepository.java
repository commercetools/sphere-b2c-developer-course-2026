package com.lifestylehomecorp.cart.application;

/**
 * Resolves a bundle product to the parent + child SKUs the cart needs — WITHOUT depending on the
 * catalog module. Module independence holds (every domain module depends only on {@code platform}),
 * so the cart owns its own minimal product read here; catalog's task 2.7 owns the PDP roll-up. The
 * two share a concept, not code.
 */
public interface BundleRepository {

    /**
     * Resolve the bundle by key. An unknown key surfaces as a commercetools {@code NotFoundException}
     * (→ HTTP 404); a product that is not a bundle resolves to empty {@link BundleSpec#components()}.
     */
    BundleSpec resolve(String bundleKey);
}
