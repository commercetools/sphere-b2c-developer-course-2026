package com.lifestylehomecorp.customer.api.dto;

import com.lifestylehomecorp.customer.domain.MergeReport;

import java.util.List;

/** What sign-in did to the guest basket — the source of the storefront's "we combined your basket" notice. */
public record MergeReportView(
        String mode,
        String activeCartId,
        String anonymousCartId,
        String anonymousCartState,
        int mergedLines,
        int addedLines,
        List<String> leftBehind) {

    public static MergeReportView from(MergeReport r) {
        return new MergeReportView(r.mode(), r.activeCartId(), r.anonymousCartId(), r.anonymousCartState(),
                r.mergedLines(), r.addedLines(), r.leftBehind());
    }
}
