package com.lifestylehomecorp.customer.domain;

import java.util.List;

/**
 * What sign-in did to the guest basket (5.3) — built from the anonymous cart AFTER sign-in, which
 * commercetools leaves in place (cartState Merged) as the audit trail of what did and did not merge.
 *
 * @param mode              the AnonymousCartSignInMode applied
 * @param activeCartId      the cart the session now points at (the customer's active cart)
 * @param anonymousCartId   the guest cart before sign-in (null if the guest had none)
 * @param anonymousCartState its state afterwards: Merged (items were merged), Active (it became the
 *                          customer's cart — UseAsNew… or a first cart), or null if there was none
 * @param mergedLines       guest lines that matched a customer line (higher quantity kept)
 * @param addedLines        guest lines added as new lines to the customer's cart
 * @param leftBehind        guest SKUs that did NOT make it (key conflicts) — still on the Merged cart
 */
public record MergeReport(
        String mode,
        String activeCartId,
        String anonymousCartId,
        String anonymousCartState,
        int mergedLines,
        int addedLines,
        List<String> leftBehind) {

    public static MergeReport none(String mode, String activeCartId) {
        return new MergeReport(mode, activeCartId, null, null, 0, 0, List.of());
    }
}
