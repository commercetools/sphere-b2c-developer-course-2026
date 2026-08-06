package com.lifestylehomecorp.catalog.api;

import com.lifestylehomecorp.catalog.api.dto.BundleView;
import com.lifestylehomecorp.catalog.api.dto.CategoryView;
import com.lifestylehomecorp.catalog.api.dto.MoneyView;
import com.lifestylehomecorp.catalog.api.dto.ProductPageView;
import com.lifestylehomecorp.catalog.api.dto.ProductView;
import com.lifestylehomecorp.catalog.api.dto.VariantMatrixView;
import com.lifestylehomecorp.catalog.domain.Bundle;
import com.lifestylehomecorp.catalog.domain.CategorySummary;
import com.lifestylehomecorp.catalog.domain.Money;
import com.lifestylehomecorp.catalog.domain.ProductPage;
import com.lifestylehomecorp.catalog.domain.ProductSummary;
import com.lifestylehomecorp.catalog.domain.VariantMatrix;

/**
 * Maps catalog domain records to their wire view models. The single translation point
 * between the domain and the HTTP contract.
 */
public final class CatalogViewMapper {

    private CatalogViewMapper() {
    }

    public static ProductView toView(ProductSummary p) {
        return new ProductView(p.key(), p.name(), p.slug(), toView(p.price()), p.imageUrl());
    }

    /** The PLP envelope: this page's cards + the total match count (Task 2.1 / 2.4). */
    public static ProductPageView toView(ProductPage page) {
        return new ProductPageView(
                page.products().stream().map(CatalogViewMapper::toView).toList(),
                page.total());
    }

    public static CategoryView toView(CategorySummary c) {
        return new CategoryView(
                c.key(),
                c.name(),
                c.slug(),
                c.children().stream().map(CatalogViewMapper::toView).toList());
    }

    public static VariantMatrixView toView(VariantMatrix m) {
        return new VariantMatrixView(
                m.key(),
                m.defaultSku(),
                m.axes().stream()
                        .map(a -> new VariantMatrixView.AxisView(a.name(), a.options()))
                        .toList(),
                m.variants().stream()
                        .map(v -> new VariantMatrixView.VariantOptionView(
                                v.sku(), v.selections(), toView(v.price()), v.imageUrl(), v.available()))
                        .toList());
    }

    public static BundleView toView(Bundle b) {
        return new BundleView(
                b.key(),
                b.name(),
                b.isBundle(),
                toView(b.totalPrice()),
                b.components().stream().map(CatalogViewMapper::toView).toList());
    }

    private static MoneyView toView(Money money) {
        return money == null ? null : new MoneyView(money.currencyCode(), money.centAmount());
    }
}
