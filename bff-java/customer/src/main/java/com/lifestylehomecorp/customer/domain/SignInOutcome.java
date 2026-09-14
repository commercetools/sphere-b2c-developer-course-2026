package com.lifestylehomecorp.customer.domain;

/** Result of sign-up / sign-in: who you are now, which cart is active, and what happened to the guest basket. */
public record SignInOutcome(
        CustomerProfile customer,
        String activeCartId,
        int cartItemCount,
        MergeReport mergeReport) {
}
