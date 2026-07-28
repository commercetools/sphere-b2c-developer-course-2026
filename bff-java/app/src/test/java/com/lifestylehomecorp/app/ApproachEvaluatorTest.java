package com.lifestylehomecorp.app;

import com.lifestylehomecorp.platform.observability.ObservedCall;
import com.lifestylehomecorp.training.telemetry.ApproachEvaluator;
import com.lifestylehomecorp.training.telemetry.ApproachExpectation;
import com.lifestylehomecorp.training.telemetry.ApproachRules;
import com.lifestylehomecorp.training.telemetry.TaskSignal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The approach-telemetry evaluator, exercised without live commercetools — proves what each flag
 * catches for Task 2.4 (Browse by category). This is the deterministic core of the trainer-only
 * "did they use the expected approach" signal (see trainer-approach-telemetry.md).
 */
class ApproachEvaluatorTest {

    private final ApproachEvaluator evaluator = new ApproachEvaluator();

    // Task 2.4's real rule, from the shipped table.
    private final ApproachExpectation task24 =
            new ApproachRules().forTask("catalog.Session 2.4");

    private static ObservedCall get(String resource, String query) {
        return new ObservedCall("GET", resource, query);
    }

    @Test
    void expectedApproach_isOk() {
        // resolve category + one filtered projections query with the categories predicate
        List<ObservedCall> calls = List.of(
                get("categories", "limit=100"),
                get("product-projections", "where=categories%28id+in+%3Aids%29&staged=false"));

        TaskSignal signal = evaluator.evaluate(task24, calls);

        assertThat(signal.status()).isEqualTo(TaskSignal.OK);
        assertThat(signal.flags()).isEmpty();
        assertThat(signal.callCount()).isEqualTo(2);
    }

    @Test
    void usingProductSearch_isFlaggedAsUnexpectedApi() {
        // the exact deviation the colleague raised: categoriesSubTree via Product Search
        List<ObservedCall> calls = List.of(
                get("categories", "limit=100"),
                get("products/search", ""));

        TaskSignal signal = evaluator.evaluate(task24, calls);

        assertThat(signal.status()).isEqualTo(TaskSignal.FLAGGED);
        assertThat(signal.flags()).anyMatch(f -> f.contains("unexpected-api: products"));
    }

    @Test
    void productSearchThenHydrate_flagsUnexpectedApiTooManyCallsAndMissingPredicate() {
        // the real Product Search demo shape: resolve category + /products/search + hydrate by id
        List<ObservedCall> calls = List.of(
                get("categories", "limit=100"),
                get("products/search", ""),
                get("product-projections", "where=id+in+%3Aids&staged=false&limit=50"));

        TaskSignal signal = evaluator.evaluate(task24, calls);

        assertThat(signal.status()).isEqualTo(TaskSignal.FLAGGED);
        assertThat(signal.flags()).anyMatch(f -> f.contains("unexpected-api: products"));
        assertThat(signal.flags()).anyMatch(f -> f.startsWith("too-many-calls: 3"));
        assertThat(signal.flags()).anyMatch(f -> f.contains("missing-predicate: categories(id in"));
    }

    @Test
    void fetchAllThenFilterInJava_isFlaggedAsMissingPredicate() {
        // projections query WITHOUT the category predicate = pulled everything, filtered client-side
        List<ObservedCall> calls = List.of(
                get("categories", "limit=100"),
                get("product-projections", "staged=false&limit=500"));

        TaskSignal signal = evaluator.evaluate(task24, calls);

        assertThat(signal.status()).isEqualTo(TaskSignal.FLAGGED);
        assertThat(signal.flags()).anyMatch(f -> f.contains("missing-predicate: categories(id in"));
    }

    @Test
    void nPlusOne_isFlaggedAsTooManyCalls() {
        List<ObservedCall> calls = List.of(
                get("categories", "limit=100"),
                get("product-projections", "where=categories%28id+in+%3Aids%29"),
                get("product-projections", "where=categories%28id+in+%3Aids%29"),
                get("product-projections", "where=categories%28id+in+%3Aids%29"));

        TaskSignal signal = evaluator.evaluate(task24, calls);

        assertThat(signal.status()).isEqualTo(TaskSignal.FLAGGED);
        assertThat(signal.flags()).anyMatch(f -> f.startsWith("too-many-calls: 4"));
    }

    @Test
    void unknownCategoryKey_onlyResolvesCategory_isOk() {
        // start == null path: categories called, no products query — must NOT false-flag
        List<ObservedCall> calls = List.of(get("categories", "limit=100"));

        TaskSignal signal = evaluator.evaluate(task24, calls);

        assertThat(signal.status()).isEqualTo(TaskSignal.OK);
    }

    @Test
    void listProducts_task21_withFullPriceContext_isOk() {
        // one projections call carrying the full contextual price selection
        ApproachExpectation task21 = new ApproachRules().forTask("catalog.Session 2.1");

        TaskSignal signal = evaluator.evaluate(task21, List.of(get("product-projections",
                "staged=false&limit=20&priceCurrency=EUR&priceCountry=DE&priceChannel=abc123")));

        assertThat(signal.status()).isEqualTo(TaskSignal.OK);
    }

    @Test
    void listProducts_task21_currencyAndCountryOnly_flagsMissingChannelPrice() {
        // the skill-typical output: priceCurrency + priceCountry but no channel — an arbitrary,
        // possibly-wrong price for the shopper's store. Passes "does it work?", must still flag.
        ApproachExpectation task21 = new ApproachRules().forTask("catalog.Session 2.1");

        TaskSignal signal = evaluator.evaluate(task21, List.of(get("product-projections",
                "staged=false&limit=20&priceCurrency=EUR&priceCountry=DE")));

        assertThat(signal.status()).isEqualTo(TaskSignal.FLAGGED);
        assertThat(signal.flags()).anyMatch(f -> f.contains("missing-predicate: priceChannel"));
    }

    // ---- get-by-key gap (2.2): fetch-all+filter hits the same resource head as the by-key GET ----

    private final ApproachExpectation task22 = new ApproachRules().forTask("catalog.Session 2.2");

    @Test
    void getByKey_task22_properByKeyLookup_isOk() {
        TaskSignal signal = evaluator.evaluate(task22,
                List.of(get("product-projections/key=chair-01",
                        "staged=false&priceCurrency=EUR&priceCountry=DE&priceChannel=abc123")));

        assertThat(signal.status()).isEqualTo(TaskSignal.OK);
    }

    @Test
    void getByKey_task22_fetchAllThenFilter_isFlaggedMissingByKeyAccess() {
        // list call (no key= in the resource) + filter in Java — passes head/count, must still flag
        TaskSignal signal = evaluator.evaluate(task22,
                List.of(get("product-projections", "staged=false&limit=200")));

        assertThat(signal.status()).isEqualTo(TaskSignal.FLAGGED);
        assertThat(signal.flags()).anyMatch(f -> f.contains("missing-by-key-access"));
    }
}
