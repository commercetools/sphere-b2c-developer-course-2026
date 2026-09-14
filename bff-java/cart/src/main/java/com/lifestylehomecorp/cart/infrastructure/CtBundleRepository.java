package com.lifestylehomecorp.cart.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductVariant;
import com.lifestylehomecorp.cart.application.BundleComponent;
import com.lifestylehomecorp.cart.application.BundleRepository;
import com.lifestylehomecorp.cart.application.BundleSpec;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a bundle product and its component SKUs via the SDK — the cart module's own product read, so
 * the cart never imports the catalog module (module independence). A bundle is modelled as a Product
 * whose {@code product-ref} attribute (a set of product references) points at its components; this
 * resolves those refs to child SKUs, preserving the bundle's declared component order.
 */
@Repository
public class CtBundleRepository implements BundleRepository {

    /** The set-of-product-reference attribute that models a bundle's components (bedding-bundle type). */
    private static final String BUNDLE_REF_ATTR = "product-ref";

    private final ProjectApiRoot apiRoot;

    public CtBundleRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public BundleSpec resolve(String bundleKey) {
        // The bundle master itself — a commercetools 404 (unknown key) surfaces as NotFoundException.
        ProductProjection bundle = apiRoot.productProjections().withKey(bundleKey).get()
                .withStaged(false)
                .executeBlocking().getBody();

        List<String> componentIds = componentIds(bundle);
        List<BundleComponent> components = new ArrayList<>();
        if (!componentIds.isEmpty()) {
            // One `id in (...)` read for all components; the result order is arbitrary, so index by id.
            List<ProductProjection> fetched = apiRoot.productProjections().get()
                    .withStaged(false)
                    .withLimit(componentIds.size())
                    .withWhere("id in :ids")
                    .withPredicateVar("ids", componentIds)
                    .executeBlocking().getBody().getResults();
            Map<String, ProductProjection> byId = new HashMap<>();
            for (ProductProjection p : fetched) {
                byId.put(p.getId(), p);
            }
            // Restore the bundle's declared component order.
            for (String id : componentIds) {
                ProductProjection p = byId.get(id);
                if (p != null && p.getMasterVariant() != null && p.getMasterVariant().getSku() != null) {
                    components.add(new BundleComponent(p.getMasterVariant().getSku(), 1L));
                }
            }
        }

        long variantId = bundle.getMasterVariant() != null && bundle.getMasterVariant().getId() != null
                ? bundle.getMasterVariant().getId() : 1L;
        return new BundleSpec(bundle.getId(), variantId, bundle.getKey(), components);
    }

    /** The component product ids referenced by a bundle's {@code product-ref} attribute (empty if none). */
    private static List<String> componentIds(ProductProjection product) {
        Object value = attributeValue(product.getMasterVariant(), BUNDLE_REF_ATTR);
        List<String> ids = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object ref : list) {
                addReferenceId(ref, ids);
            }
        } else if (value != null) {
            addReferenceId(value, ids);
        }
        return ids;
    }

    private static Object attributeValue(ProductVariant variant, String name) {
        if (variant == null || variant.getAttributes() == null) {
            return null;
        }
        return variant.getAttributes().stream()
                .filter(a -> name.equals(a.getName()))
                .map(Attribute::getValue)
                .findFirst().orElse(null);
    }

    private static void addReferenceId(Object ref, List<String> ids) {
        // The SDK deserializes a product-reference attribute value to a typed Reference.
        if (ref instanceof Reference r && r.getId() != null) {
            ids.add(r.getId());
        } else if (ref instanceof Map<?, ?> m && m.get("id") != null) {
            ids.add(m.get("id").toString());
        }
    }
}
