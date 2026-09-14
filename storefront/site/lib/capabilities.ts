/**
 * The single capabilities map. Each storefront feature is gated on one capability flag; the BFF's
 * GET /api/training/capabilities reports which are `unlocked`. This local map adds the human context
 * (session number + name, task label, endpoint) used by the "Unlock" dialog and the status board.
 *
 * Keep in sync with the BFF capability registry and the course task list.
 */
export interface CapabilityMeta {
  capability: string;
  /** e.g. "Session 2" */
  session: string;
  /** e.g. "The Catalogue" */
  sessionName: string;
  taskLabel: string;
  bffEndpoint: string;
}

// The course's session codenames — keep in step with planning/SESSION_PLAN.md ("At a glance").
const SESSION_NAMES: Record<string, string> = {
  'Session 1': 'Ignition — Platform, SDK & Project',
  'Session 2': 'The Catalogue',
  'Session 3': 'Find It — Store-Scoped Discovery',
  'Session 4': 'Fill the Basket — The Cart',
  'Session 5': 'Know Your Customer — Identity',
  'Session 6': 'Close the Deal — Checkout & Orders',
  'Session 7': 'Beyond the BFF — Extensibility',
  'Session 8': 'Sync the Catalogue — Import',
  'Session 9': 'Connect',
};

/** Session display order for the status board. */
export const SESSION_ORDER = Object.keys(SESSION_NAMES);

export function sessionName(session: string): string {
  return SESSION_NAMES[session] ?? '';
}

function meta(capability: string, session: string, taskLabel: string, bffEndpoint: string): CapabilityMeta {
  return { capability, session, sessionName: SESSION_NAMES[session] ?? '', taskLabel, bffEndpoint };
}

export const CAPABILITY_META: Record<string, CapabilityMeta> = {
  'project.info':          meta('project.info',          'Session 1', 'Return project settings',        'GET /api/project'),
  'distribution.stores':   meta('distribution.stores',   'Session 1', 'List stores',                    'GET /api/stores'),
  'distribution.storeByKey': meta('distribution.storeByKey', 'Session 1', 'Get store by key',           'GET /api/stores/{key}'),
  'distribution.store':    meta('distribution.store',    'Session 1', 'Store / region switch',          'GET /api/store-context'),
  'catalog.plp':            meta('catalog.plp',            'Session 2', 'List products (PLP)',            'GET /api/products'),
  'catalog.pdp':           meta('catalog.pdp',           'Session 2', 'Get product by key',             'GET /api/products/{key}'),
  'catalog.categories':    meta('catalog.categories',    'Session 2', 'List categories',                'GET /api/categories'),
  'catalog.categoryProducts': meta('catalog.categoryProducts', 'Session 2', 'Browse by category',        'GET /api/categories/{key}/products'),
  'catalog.variantMatrix': meta('catalog.variantMatrix', 'Session 2', 'Variant selection matrix',       'GET /api/products/{key}/variants'),
  'catalog.bundles':       meta('catalog.bundles',       'Session 2', 'Bundle / composite resolution',  'GET /api/products/{key}/bundle'),
  'catalog.localeSlugs':   meta('catalog.localeSlugs',   'Session 2', 'Locale fallback + slug routing', 'GET /api/products/by-slug/{slug}'),
  'pricing.resolve':       meta('pricing.resolve',       'Session 2', 'Scoped price + discounts',       'GET /api/products'),
  'search.query':          meta('search.query',          'Session 3', 'Store-scoped search',            'GET /api/products/search?store='),
  'catalog.pdpInStore':    meta('catalog.pdpInStore',    'Session 3', 'In-store product detail',        'GET /api/products/{key}?store='),
  'search.fullText':       meta('search.fullText',       'Session 3', 'Full-text search',               'GET /api/search?store=&q='),
  'search.byCategory':     meta('search.byCategory',     'Session 3', 'Browse by category (subtree)',   'GET /api/products/search?store=&category='),
  'search.facets':         meta('search.facets',         'Session 3', 'Faceted search',                 'GET /api/products/facets?store='),
  'search.plpV2':          meta('search.plpV2',          'Session 3', 'Storefront PLP (compose)',       'GET /api/plp?store='),
  'search.postFilter':     meta('search.postFilter',     'Session 3', 'Facet post-filtering',           'GET /api/plp?store=&filter='),
  'cart.create':           meta('cart.create',           'Session 4', 'Create the guest cart',          'POST /api/cart'),
  'cart.lineItems':        meta('cart.lineItems',        'Session 4', 'Add / manage line items',        'POST·PATCH·DELETE /api/cart/line-items'),
  'cart.bundles':          meta('cart.bundles',          'Session 4', 'Add a bundle to the cart',       'POST /api/cart/bundles'),
  'cart.recurring':        meta('cart.recurring',        'Session 4', 'Subscription line item',         'POST /api/cart/line-items?recurring'),
  'cart.channel':          meta('cart.channel',          'Session 4', 'Fulfilment: pickup vs delivery', 'PUT /api/cart/line-items/{id}/channel'),
  'cart.stockGate':        meta('cart.stockGate',        'Session 4', 'Inventory modes + stock gate',   'PUT /api/cart/line-items/{id}/inventory-mode'),
  'cart.address':          meta('cart.address',          'Session 4', 'Shipping address',               'PUT /api/cart/shipping-address'),
  'cart.shipping':         meta('cart.shipping',         'Session 4', 'Shipping method: match & select','GET /api/cart/shipping-methods'),
  'cart.promo':            meta('cart.promo',            'Session 4', 'Apply a discount code',          'POST /api/cart/discount-codes'),
  'cart.summary':          meta('cart.summary',          'Session 4', 'Cart summary + savings',         'GET /api/cart'),
  'cart.shoppingList':     meta('cart.shoppingList',     'Session 4', 'Shopping list (wishlist)',       'GET·POST /api/shopping-list'),
  'customer.register':     meta('customer.register',     'Session 5', 'Register (sign up)',             'POST /api/customers/signup'),
  'customer.login':        meta('customer.login',        'Session 5', 'Sign in (login)',                'POST /api/customers/login'),
  'customer.cartMerge':    meta('customer.cartMerge',    'Session 5', 'Guest → customer cart merge',    'POST /api/customers/login?mergeMode='),
  'customer.profile':      meta('customer.profile',      'Session 5', 'My profile: read & update',      'GET·PATCH /api/customers/me'),
  'customer.addresses':    meta('customer.addresses',    'Session 5', 'Address book + defaults',        'POST /api/customers/me/addresses'),
  'customer.groups':       meta('customer.groups',       'Session 5', 'Customer group → customer price','GET /api/price-context'),
  'customer.password':     meta('customer.password',     'Session 5', 'Password: change & reset',       'POST /api/customers/me/password'),
  'customer.emailVerify':  meta('customer.emailVerify',  'Session 5', 'Email verification',             'POST /api/customers/email/confirm'),
  'customer.session':      meta('customer.session',      'Session 5', 'Identity boundary (Me API vs BFF)','GET /api/session'),
  'customer.pii':          meta('customer.pii',          'Session 5', 'PII & GDPR: delete my account',  'DELETE /api/customers/me'),
  'order.create':          meta('order.create',          'Session 6', 'Checkout + order placement',     'POST /api/orders'),
  'inventory.read':        meta('inventory.read',        'Session 7', 'Availability badges',            'GET /api/inventory/{sku}'),
};

export const ALL_CAPABILITIES: CapabilityMeta[] = Object.values(CAPABILITY_META);

/** "Unlocks in Session 2 — The Catalogue" */
export function unlockHeadline(m: CapabilityMeta): string {
  return `Unlocks in ${m.session}${m.sessionName ? ` — ${m.sessionName}` : ''}`;
}

/** Capabilities grouped by session, in course order, for the status board. */
export function capabilitiesBySession(): { session: string; sessionName: string; items: CapabilityMeta[] }[] {
  return SESSION_ORDER.map((session) => ({
    session,
    sessionName: SESSION_NAMES[session] ?? '',
    items: ALL_CAPABILITIES.filter((c) => c.session === session),
  })).filter((g) => g.items.length > 0);
}
