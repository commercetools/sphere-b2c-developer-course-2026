package com.lifestylehomecorp.cart.application;

import com.commercetools.api.models.cart.Cart;
import com.commercetools.api.models.cart.LineItem;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.common.TypedMoney;
import com.commercetools.api.models.recurrence_policy.RecurrencePolicy;
import com.commercetools.api.models.recurrence_policy.StandardSchedule;
import com.commercetools.api.models.recurring_order.LineItemRecurrenceInfo;
import com.commercetools.api.models.shipping_method.ShippingMethod;
import com.lifestylehomecorp.cart.domain.CartLine;
import com.lifestylehomecorp.cart.domain.CartSummary;
import com.lifestylehomecorp.cart.domain.Money;
import com.lifestylehomecorp.cart.domain.ShippingOption;

import java.util.ArrayList;
import java.util.List;

/** Maps the raw SDK {@link Cart} into the SDK-free {@link CartSummary} the storefront renders. */
public final class CartMapper {

    private CartMapper() {
    }

    public static CartSummary toDomain(Cart cart) {
        String currency = cart.getTotalPrice() != null ? cart.getTotalPrice().getCurrencyCode() : null;
        int fractionDigits = cart.getTotalPrice() != null ? cart.getTotalPrice().getFractionDigits() : 2;

        List<CartLine> lines = new ArrayList<>();
        long subtotalCents = 0;
        long savingsCents = 0;
        for (LineItem li : cart.getLineItems()) {
            long qty = li.getQuantity();
            Money unit = li.getPrice() != null ? money(li.getPrice().getValue()) : null;
            Money lineTotal = money(li.getTotalPrice());
            long original = unit != null ? unit.centAmount() * qty : (lineTotal != null ? lineTotal.centAmount() : 0);
            long discounted = lineTotal != null ? lineTotal.centAmount() : original;
            long lineSaving = Math.max(0, original - discounted);
            subtotalCents += original;
            savingsCents += lineSaving;

            LineItemRecurrenceInfo recurrence = li.getRecurrenceInfo();
            lines.add(new CartLine(
                    li.getId(),
                    li.getVariant() != null ? li.getVariant().getSku() : null,
                    localized(li.getName(), li.getVariant() != null ? li.getVariant().getSku() : null),
                    qty,
                    unit,
                    lineTotal,
                    new Money(lineSaving, currency, fractionDigits),
                    customString(li, "bundleId"),
                    customString(li, "parentId"),
                    recurrence != null,
                    recurrence != null ? recurrenceLabel(recurrence) : null,
                    recurrence != null && recurrence.getPriceSelectionMode() != null
                            ? recurrence.getPriceSelectionMode().getJsonName() : null,
                    li.getDistributionChannel() != null ? li.getDistributionChannel().getId() : null,
                    li.getSupplyChannel() != null ? li.getSupplyChannel().getId() : null,
                    li.getInventoryMode() != null ? li.getInventoryMode().getJsonName() : null));
        }

        // Cart-level discount (discount codes / whole-cart cart discounts) adds to savings.
        if (cart.getDiscountOnTotalPrice() != null && cart.getDiscountOnTotalPrice().getDiscountedAmount() != null) {
            savingsCents += cart.getDiscountOnTotalPrice().getDiscountedAmount().getCentAmount();
        }

        Money shipping = cart.getShippingInfo() != null ? money(cart.getShippingInfo().getPrice()) : null;
        Money tax = (cart.getTaxedPrice() != null && cart.getTaxedPrice().getTotalTax() != null)
                ? money(cart.getTaxedPrice().getTotalTax()) : null;

        // A bundle counts as one item: children (those carrying a parentId) are excluded from the badge.
        int itemCount = cart.getLineItems().stream()
                .filter(li -> customString(li, "parentId") == null)
                .mapToInt(li -> li.getQuantity().intValue())
                .sum();

        List<String> codes = new ArrayList<>();
        if (cart.getDiscountCodes() != null) {
            cart.getDiscountCodes().forEach(dc -> {
                if (dc.getDiscountCode() != null && dc.getDiscountCode().getObj() != null
                        && dc.getDiscountCode().getObj().getCode() != null) {
                    codes.add(dc.getDiscountCode().getObj().getCode());
                }
            });
        }

        return new CartSummary(
                cart.getId(),
                cart.getVersion(),
                currency,
                cart.getCountry(),
                lines,
                itemCount,
                new Money(subtotalCents, currency, fractionDigits),
                new Money(savingsCents, currency, fractionDigits),
                shipping,
                tax,
                money(cart.getTotalPrice()),
                cart.getShippingInfo() != null ? cart.getShippingInfo().getShippingMethodName() : null,
                codes);
    }

    /** 4.8 — a matching shipping method → the SDK-free option. */
    public static ShippingOption toShippingOption(ShippingMethod m) {
        return new ShippingOption(m.getId(), m.getKey(), m.getName(), Boolean.TRUE.equals(m.getIsDefault()));
    }

    /**
     * A human frequency for a recurring line — "Every month" / "Every 2 weeks" — derived from the
     * (expanded) RecurrencePolicy's schedule, falling back to its name, then a generic label.
     */
    private static String recurrenceLabel(LineItemRecurrenceInfo recurrence) {
        RecurrencePolicy policy = recurrence.getRecurrencePolicy() != null
                ? recurrence.getRecurrencePolicy().getObj() : null;
        if (policy == null) {
            return "Recurring";
        }
        if (policy.getSchedule() instanceof StandardSchedule schedule && schedule.getIntervalUnit() != null) {
            long value = schedule.getValue() != null ? schedule.getValue() : 1L;
            String unit = switch (schedule.getIntervalUnit().getJsonName()) {
                case "Days" -> "day";
                case "Weeks" -> "week";
                case "Months" -> "month";
                default -> "cycle";
            };
            return value == 1 ? "Every " + unit : "Every " + value + " " + unit + "s";
        }
        return localized(policy.getName(), "Recurring");
    }

    private static Money money(TypedMoney m) {
        return m == null ? null : new Money(m.getCentAmount(), m.getCurrencyCode(), m.getFractionDigits());
    }

    private static String localized(LocalizedString name, String fallback) {
        if (name == null) {
            return fallback;
        }
        for (String locale : List.of("en-GB", "en-US", "en", "de-DE")) {
            String v = name.get(locale);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return name.values().values().stream().findFirst().orElse(fallback);
    }

    /** Read a String custom field off a line item ({@code bundleId} / {@code parentId}); null if absent. */
    static String customString(LineItem li, String field) {
        if (li.getCustom() == null || li.getCustom().getFields() == null) {
            return null;
        }
        Object v = li.getCustom().getFields().values().get(field);
        return v == null ? null : v.toString();
    }
}
