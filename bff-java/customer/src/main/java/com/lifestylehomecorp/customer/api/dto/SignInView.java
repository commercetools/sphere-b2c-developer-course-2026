package com.lifestylehomecorp.customer.api.dto;

import com.lifestylehomecorp.customer.domain.SignInOutcome;

/** Response of sign-up / sign-in: the customer, the now-active cart, and the merge report. */
public record SignInView(CustomerView customer, String activeCartId, int cartItemCount, MergeReportView mergeReport) {

    public static SignInView from(SignInOutcome o) {
        return new SignInView(CustomerView.from(o.customer()), o.activeCartId(), o.cartItemCount(),
                MergeReportView.from(o.mergeReport()));
    }
}
