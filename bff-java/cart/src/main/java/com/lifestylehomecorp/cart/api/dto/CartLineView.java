package com.lifestylehomecorp.cart.api.dto;

import com.lifestylehomecorp.cart.domain.CartLine;

import java.util.List;

/**
 * A cart line on the wire. A bundle is returned as ONE parent line with its component lines nested in
 * {@code children} (and the parent's money rolled up from them), so the response reads like the cart
 * the shopper sees — {@code lines.length} then equals {@code itemCount}. Ordinary lines and bundle
 * children carry an empty {@code children}.
 */
public record CartLineView(
        String lineItemId,
        String sku,
        String name,
        long quantity,
        MoneyView unitPrice,
        MoneyView lineTotal,
        MoneyView lineSavings,
        String bundleId,
        String parentKey,
        boolean recurring,
        String recurrenceLabel,
        String recurrencePriceMode,
        String distributionChannelId,
        String supplyChannelId,
        String inventoryMode,
        List<CartLineView> children) {

    /** A plain line (ordinary product, subscription, or a bundle child) — no nested children. */
    public static CartLineView from(CartLine l) {
        return new CartLineView(
                l.lineItemId(), l.sku(), l.name(), l.quantity(),
                MoneyView.from(l.unitPrice()), MoneyView.from(l.lineTotal()), MoneyView.from(l.lineSavings()),
                l.bundleId(), l.parentKey(), l.recurring(), l.recurrenceLabel(), l.recurrencePriceMode(),
                l.distributionChannelId(), l.supplyChannelId(), l.inventoryMode(),
                List.of());
    }

    /**
     * A bundle PARENT with its children nested and its displayed money rolled up from them (the parent
     * line is priced 0 in commercetools; the total/saving live on the children).
     */
    public static CartLineView bundleParent(CartLine parent, MoneyView rolledTotal, MoneyView rolledSavings,
                                            List<CartLineView> children) {
        return new CartLineView(
                parent.lineItemId(), parent.sku(), parent.name(), parent.quantity(),
                rolledTotal, rolledTotal, rolledSavings,
                parent.bundleId(), parent.parentKey(), parent.recurring(),
                parent.recurrenceLabel(), parent.recurrencePriceMode(),
                parent.distributionChannelId(), parent.supplyChannelId(), parent.inventoryMode(),
                children);
    }
}
