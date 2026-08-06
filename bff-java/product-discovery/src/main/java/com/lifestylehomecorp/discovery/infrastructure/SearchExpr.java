package com.lifestylehomecorp.discovery.infrastructure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * An immutable, composable model of the Product Search query language ({@code SearchQueryInput}).
 * Each node renders itself as the {@code Map<String,Object>} value for a GraphQL variable, so the query
 * document stays constant and no caller value is ever concatenated into query text.
 *
 * <p>Covers the full Product Search expression set: the logical operators {@code and}/{@code or}/
 * {@code not}/{@code filter} plus every leaf expression — {@code exists}, {@code exact}, {@code prefix},
 * {@code wildcard}, {@code fullText}, {@code fullTextPrefix}, {@code fuzzy}, and {@code range} (all
 * numeric/temporal domains via {@link RangeType}). Sealed so the set is closed and exhaustively
 * switchable; the {@link #and}/{@link #or} factories fold the "no single expression in [and]/[or]"
 * runtime rule into ONE place.
 *
 * <p><b>Noted improvement (deferred):</b> {@code fieldType} is carried as a plain {@code String}, but it
 * is really the GraphQL {@code SearchFieldType} enum (22 values: {@code text}/{@code ltext}/{@code enum}/
 * {@code lenum}/{@code number}/{@code money}/{@code date}/{@code datetime}/{@code time}/{@code reference}
 * plus the {@code set_*} variants). A small local enum — each constant carrying its wire spelling, e.g.
 * {@code LENUM("lenum")} — would make it compile-time safe here AND in {@link
 * com.lifestylehomecorp.discovery.application.FacetSpec} (Task 3.8's participant-derived facets), which is
 * where the value is actually chosen; today a typo like {@code "Lenum"} only fails server-side at request
 * time. Strings work, so this is deferred hardening, not a fix.
 */
sealed interface SearchExpr
        permits SearchExpr.And, SearchExpr.Or, SearchExpr.Not, SearchExpr.Filter,
                SearchExpr.Exists, SearchExpr.Exact, SearchExpr.Prefix, SearchExpr.Wildcard,
                SearchExpr.FullText, SearchExpr.FullTextPrefix, SearchExpr.Fuzzy, SearchExpr.Range {

    /** The {@code SearchQueryInput}-shaped value for a GraphQL variable. */
    Map<String, Object> toValue();

    /** Combine with {@code and} (bare when one clause, {@code null} when none). */
    static SearchExpr and(List<SearchExpr> parts) {
        return combine(parts, And::new);
    }

    /** Combine with {@code or} (bare when one clause, {@code null} when none). */
    static SearchExpr or(List<SearchExpr> parts) {
        return combine(parts, Or::new);
    }

    /** Convenience factory for the common numeric ({@code long}, minor units) range — e.g. price. */
    static SearchExpr longRange(String field, Long gte, Long lt) {
        return new Range(RangeType.LONG, field, gte, null, null, lt);
    }

    private static SearchExpr combine(List<SearchExpr> parts, Function<List<SearchExpr>, SearchExpr> wrap) {
        List<SearchExpr> present = new ArrayList<>();
        for (SearchExpr p : parts) {
            if (p != null) {
                present.add(p);
            }
        }
        return switch (present.size()) {
            case 0 -> null;
            case 1 -> present.get(0);
            default -> wrap.apply(present);
        };
    }

    private static List<Map<String, Object>> values(List<SearchExpr> parts) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SearchExpr p : parts) {
            out.add(p.toValue());
        }
        return out;
    }

    record And(List<SearchExpr> parts) implements SearchExpr {
        public Map<String, Object> toValue() {
            return Map.of("and", SearchExpr.values(parts));
        }
    }

    record Or(List<SearchExpr> parts) implements SearchExpr {
        public Map<String, Object> toValue() {
            return Map.of("or", SearchExpr.values(parts));
        }
    }

    record Not(List<SearchExpr> parts) implements SearchExpr {
        public Map<String, Object> toValue() {
            return Map.of("not", SearchExpr.values(parts));
        }
    }

    /** Wraps clauses in a {@code filter} array (unscored) — used for the category subtree. */
    record Filter(List<SearchExpr> parts) implements SearchExpr {
        public Map<String, Object> toValue() {
            return Map.of("filter", SearchExpr.values(parts));
        }
    }

    /** {@code exists} — the field has a non-null value. */
    record Exists(String field, String fieldType, String language) implements SearchExpr {
        public Map<String, Object> toValue() {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("field", field);
            if (notBlank(fieldType)) {
                e.put("fieldType", fieldType);
            }
            if (notBlank(language)) {
                e.put("language", language);
            }
            return Map.of("exists", e);
        }
    }

    /** {@code exact} — exact match on the field's value. */
    record Exact(String field, Object value, String fieldType, String language) implements SearchExpr {
        public Map<String, Object> toValue() {
            return Map.of("exact", anyValue(field, value, fieldType, language));
        }
    }

    /** {@code prefix} — values starting with the given prefix. */
    record Prefix(String field, Object value, String fieldType, String language) implements SearchExpr {
        public Map<String, Object> toValue() {
            return Map.of("prefix", anyValue(field, value, fieldType, language));
        }
    }

    /** {@code wildcard} — values matching the given wildcard pattern (e.g. {@code "be*"}). */
    record Wildcard(String field, Object value, String fieldType, String language) implements SearchExpr {
        public Map<String, Object> toValue() {
            return Map.of("wildcard", anyValue(field, value, fieldType, language));
        }
    }

    /** {@code fullText} — full-text search on the field. */
    record FullText(String field, String value, String language) implements SearchExpr {
        public Map<String, Object> toValue() {
            return Map.of("fullText", fullTextValue(field, value, language));
        }
    }

    /** {@code fullTextPrefix} — full-text match treating the last token as a prefix (type-ahead). */
    record FullTextPrefix(String field, String value, String language) implements SearchExpr {
        public Map<String, Object> toValue() {
            return Map.of("fullTextPrefix", fullTextValue(field, value, language));
        }
    }

    record Fuzzy(String field, String value, int level, String language) implements SearchExpr {
        public Map<String, Object> toValue() {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("field", field);
            f.put("value", value);
            f.put("level", level);
            if (notBlank(language)) {
                f.put("language", language);
            }
            return Map.of("fuzzy", f);
        }
    }

    /** The value domain of a {@code range} expression — maps to the schema's per-type range keys. */
    enum RangeType {
        LONG("long"), FLOAT("float"), DATE("date"), DATETIME("datetime"), TIME("time");

        private final String key;

        RangeType(String key) {
            this.key = key;
        }
    }

    /**
     * {@code range} — values within a bound range. {@code type} selects the value domain; each bound
     * ({@code gte}/{@code gt}/{@code lte}/{@code lt}) is optional and emitted only when present. A bound
     * is the scalar its type expects: a number for {@code long}/{@code float}; an ISO-8601 string for
     * {@code date}/{@code datetime}/{@code time}.
     */
    record Range(RangeType type, String field, Object gte, Object gt, Object lte, Object lt)
            implements SearchExpr {
        public Map<String, Object> toValue() {
            Map<String, Object> bounds = new LinkedHashMap<>();
            bounds.put("field", field);
            if (gte != null) {
                bounds.put("gte", gte);
            }
            if (gt != null) {
                bounds.put("gt", gt);
            }
            if (lte != null) {
                bounds.put("lte", lte);
            }
            if (lt != null) {
                bounds.put("lt", lt);
            }
            return Map.of("range", Map.of(type.key, bounds));
        }
    }

    /** Shared body for {@code exact}/{@code prefix}/{@code wildcard} (SearchAnyValueExpressionInput). */
    private static Map<String, Object> anyValue(String field, Object value, String fieldType, String language) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("field", field);
        v.put("value", value);
        if (notBlank(fieldType)) {
            v.put("fieldType", fieldType);
        }
        if (notBlank(language)) {
            v.put("language", language);
        }
        return v;
    }

    /** Shared body for {@code fullText}/{@code fullTextPrefix}. */
    private static Map<String, Object> fullTextValue(String field, String value, String language) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("field", field);
        f.put("value", value);
        if (notBlank(language)) {
            f.put("language", language);
        }
        return f;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
