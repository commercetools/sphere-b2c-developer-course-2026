package com.lifestylehomecorp.discovery.application;

/**
 * Resolves a category <em>key</em> to its <em>id</em> for the Product Search {@code categoriesSubTree}
 * filter. The storefront nav carries category keys (stable, human-chosen), but the search filter matches
 * on the category id — this is the small key→id hop, cached like the store/channel resolvers.
 */
public interface CategoryRepository {

    /** The category id for the given key, or {@code null} if the key is null/blank or unknown. */
    String idByKey(String key);
}
