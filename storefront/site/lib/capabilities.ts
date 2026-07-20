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

const SESSION_NAMES: Record<string, string> = {
  'Session 1': 'Platform, SDK & Project',
  'Session 2': 'The Catalogue',
  'Session 3': 'Search & Discovery',
  'Session 4': 'The Cart',
  'Session 5': 'Checkout & Orders',
  'Session 6': 'Customers & Identity',
  'Session 7': 'Inventory & Availability',
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
  'search.sort':           meta('search.sort',           'Session 3', 'Sort products',                  'GET /api/products?sort='),
  'search.facets':         meta('search.facets',         'Session 3', 'Facet / filter panel',           'GET /api/products'),
  'cart.write':            meta('cart.write',            'Session 4', 'Cart add/update/remove',         'POST /api/cart'),
  'cart.idempotent':       meta('cart.idempotent',       'Session 4', 'Version-conflict-safe mutations','POST /api/cart'),
  'order.create':          meta('order.create',          'Session 5', 'Checkout + order placement',     'POST /api/orders'),
  'customer.identity':     meta('customer.identity',     'Session 6', 'Login / register / account',     'POST /api/customers/login'),
  'customer.cartMerge':    meta('customer.cartMerge',    'Session 6', 'Anonymous → customer cart merge','POST /api/cart/merge'),
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
