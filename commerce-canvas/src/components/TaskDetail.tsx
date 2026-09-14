import { useState } from 'react';
import { TaskItem, QueryParam, buildQuery, endpointParams, resolveEndpoint } from '../api/bff';
import { methodColor, taskRef, tierClasses } from '../lib/ui';
import { Check, Lightbulb, TriangleAlert, Info, ListChecks, X } from '../lib/icons';

/** Params the catalog reads understand — offered as quick-add chips in the Try It playground. */
const CONTEXT_PARAMS = ['locale', 'priceCurrency', 'priceCountry', 'priceChannel', 'priceCustomerGroup'];
const PARAM_HINTS: Record<string, string> = {
  locale: 'en-US',
  priceCurrency: 'USD',
  priceCountry: 'US',
  priceChannel: '<channel key>',
  priceCustomerGroup: '<group id>',
};

/**
 * Default query for a task so the Canvas call matches what the storefront sends (en-US / USD / US) —
 * otherwise the BFF falls back to the first-available locale and a null price, which looks broken next
 * to the storefront. Keyed on the **capability** (not the endpoint), because 2.2 and 3.2 share
 * `/api/products/{key}` — only 3.2 (the in-store PDP) is store-scoped. Every seeded value is editable.
 *
 * - **Store-scoped reads** (`search.*` + `catalog.pdpInStore`) → default `store=b2c-retail-store` (the
 *   whole-catalogue store, so cards always render) + price context; change the store to see the
 *   assortment switch.
 * - **Catalog product reads** → full price-selection context · **category tree** → a locale · else empty.
 */
function seedQuery(task: TaskItem): QueryParam[] {
  const locale: QueryParam = { key: 'locale', value: 'en-US' };
  const priced: QueryParam[] = [
    locale,
    { key: 'priceCurrency', value: 'USD' },
    { key: 'priceCountry', value: 'US' },
  ];
  const cap = task.capability;
  // Store-scoped discovery + in-store reads: demo store + price context, PLUS the one query param each
  // endpoint needs to route to ITS task (?q= / ?category= / ?dynamic= / ?filter=). Without it the call
  // matches a sibling endpoint and the wrong task completes (e.g. no ?filter → hits 3.6, not 3.7).
  if (cap.startsWith('search.') || cap === 'catalog.pdpInStore') {
    const base: QueryParam[] = [{ key: 'store', value: 'b2c-retail-store' }, ...priced];
    switch (cap) {
      case 'search.fullText':
        return [...base, { key: 'q', value: 'chair' }];
      case 'search.byCategory':
        return [...base, { key: 'category', value: 'furniture' }];
      case 'search.facetsConfig':
        return [...base, { key: 'dynamic', value: 'true' }];
      case 'search.postFilter':
        return [...base, { key: 'filter', value: 'colour:white' }];
      default:
        return base; // search.query / search.facets / search.plpV2 / catalog.pdpInStore
    }
  }
  // Session 4 write tasks. 4.1 create takes NO body — it reads the cart context from query params
  // (currency / country, optional store); seed them so the call is self-explanatory. The recurring add
  // shares POST /api/cart/line-items with 4.2, distinguished only by ?recurring — without it the call
  // routes to 4.2 (the plain add) and the wrong task completes.
  if (cap === 'cart.create') return [{ key: 'currency', value: 'EUR' }, { key: 'country', value: 'DE' }];
  if (cap === 'cart.recurring') return [{ key: 'recurring', value: 'true' }];
  // Session 5. 5.3 shares POST /api/customers/login with 5.2, distinguished only by ?mergeMode — without
  // it the call routes to 5.2 (the plain sign-in) and the wrong task completes. 5.6 echoes the price
  // context the catalogue applies, so seed the same currency / country the storefront sends.
  if (cap === 'customer.cartMerge') return [{ key: 'mergeMode', value: 'MergeWithExistingCustomerCart' }];
  if (cap === 'customer.groups') return [{ key: 'priceCurrency', value: 'USD' }, { key: 'priceCountry', value: 'US' }];
  if (cap === 'distribution.store') return [{ key: 'store', value: 'b2c-retail-store' }];
  if (cap === 'catalog.categories') return [locale];
  if (cap.startsWith('catalog.')) return priced;
  // Fallback for anything unmapped: the old endpoint heuristic.
  if (/\bproducts\b/.test(task.endpoint)) return priced;
  if (/\bcategories\b/.test(task.endpoint)) return [locale];
  return [];
}

/**
 * Default PATH params ({key}/{slug}) per task so a by-key / by-slug read resolves against real seed data
 * instead of an empty input. Every value is a real key/slug from the b2c-developer-2026 sample catalogue
 * and stays editable in the playground.
 */
function seedValues(task: TaskItem): Record<string, string> {
  switch (task.capability) {
    case 'distribution.storeByKey':
      return { key: 'b2c-retail-store' };
    case 'catalog.pdp':
    case 'catalog.pdpInStore':
    case 'catalog.variantMatrix':
      return { key: 'charlie-armchair' };
    case 'catalog.categoryProducts':
      return { key: 'furniture' };
    case 'catalog.bundles':
      return { key: 'bedding-bundle' };
    case 'catalog.localeSlugs':
      return { slug: 'charlie-armchair' };
    default:
      return {};
  }
}

/**
 * Default JSON request body per write task (Session 4 onward), so a POST/PUT/PATCH is demoable straight
 * from Try It. Values are real keys/SKUs/codes from the b2c-developer-2026 sample data and stay editable.
 * Tasks that take only query params (e.g. 4.1 create) return '' — no body is sent.
 */
function seedBody(task: TaskItem): string {
  const j = (o: unknown) => JSON.stringify(o, null, 2);
  switch (task.capability) {
    case 'cart.lineItems':
      return j({ sku: 'CARM-023', quantity: 1 });
    case 'cart.recurring':
      return j({ sku: 'CARM-023' });
    case 'cart.bundles':
      return j({ bundleKey: 'bedding-bundle' });
    case 'cart.channel':
      return j({ distributionChannelKey: 'distribution-channel', supplyChannelKey: 'pickup-store' });
    case 'cart.stockGate':
      return j({ inventoryMode: 'ReserveOnOrder' });
    case 'cart.address':
      return j({
        country: 'DE',
        firstName: 'Alex',
        lastName: 'Doe',
        streetName: 'Hauptstraße',
        streetNumber: '1',
        postalCode: '10115',
        city: 'Berlin',
      });
    case 'cart.promo':
      return j({ code: 'SAVE10' });
    // Session 5 — identity. The demo account is the one seeded before the session (pre-5.x setup);
    // sign-up wants a fresh email each run, so the seed carries a placeholder to edit.
    case 'customer.register':
      return j({ email: 'new.shopper@lhc.test', password: 'Lhc-demo-2026!', firstName: 'New', lastName: 'Shopper' });
    case 'customer.login':
    case 'customer.cartMerge':
      return j({ email: 'ada@lhc.test', password: 'Lhc-demo-2026!' });
    case 'customer.addresses':
      return j({
        key: 'home',
        country: 'DE',
        firstName: 'Ada',
        lastName: 'Lovelace',
        streetName: 'Hauptstraße',
        streetNumber: '1',
        postalCode: '10115',
        city: 'Berlin',
        defaultShipping: true,
      });
    case 'customer.password':
      return j({ currentPassword: 'Lhc-demo-2026!', newPassword: 'Lhc-demo-2026!' });
    case 'customer.emailVerify':
      return j({ tokenValue: '<paste the value from POST /api/customers/email/token>' });
    default:
      return '';
  }
}

/** Main panel: full detail of the selected task + Try It. */
export function TaskDetail({
  task,
  completed,
  loading,
  storefrontUrl,
  onTry,
}: {
  task: TaskItem;
  completed: boolean;
  loading: boolean;
  storefrontUrl: string;
  onTry: (task: TaskItem, method: string, endpoint: string, body?: string) => void;
}) {
  const params = endpointParams(task.endpoint);
  const [values, setValues] = useState<Record<string, string>>(() => seedValues(task));
  const [query, setQuery] = useState<QueryParam[]>(() => seedQuery(task));
  const [body, setBody] = useState<string>(() => seedBody(task));
  const isWrite = /^(POST|PUT|PATCH)$/i.test(task.httpMethod);
  // Only write tasks that actually take a JSON body get the body box; query-only writes (e.g. 4.1
  // create, which reads currency/country from the query) don't — they'd only show a misleading "{}".
  const takesBody = isWrite && seedBody(task).trim() !== '';
  const resolved = resolveEndpoint(task.endpoint, values) + buildQuery(query);
  const missing = params.filter((p) => !(values[p] ?? '').trim());
  const mc = methodColor(task.httpMethod);

  const addParam = (key = '') =>
    setQuery((q) => (key && q.some((r) => r.key === key) ? q : [...q, { key, value: '' }]));
  const setParam = (i: number, patch: Partial<QueryParam>) =>
    setQuery((q) => q.map((r, idx) => (idx === i ? { ...r, ...patch } : r)));
  const removeParam = (i: number) => setQuery((q) => q.filter((_, idx) => idx !== i));

  return (
    <div className="mx-auto max-w-3xl space-y-6 p-6">
      {/* header */}
      <div>
        <div className="flex flex-wrap items-center gap-3">
          <span
            className="rounded px-2 py-0.5 text-xs font-semibold"
            style={{ background: `${mc}22`, color: mc }}
          >
            {task.httpMethod}
          </span>
          <h1 className="text-xl font-semibold" style={{ fontFamily: 'var(--font-display)' }}>
            <span className="text-[var(--color-text-muted)]">{taskRef(task)} </span>
            {task.title}
          </h1>
          <span className={`rounded px-1.5 py-0.5 text-[10px] font-semibold ${tierClasses(task.tier)}`}>
            {task.tier}
          </span>
          {completed ? (
            <span className="rounded-full bg-[var(--color-teal)]/20 px-2 py-0.5 text-xs text-[var(--color-teal-light)]">
              <Check size={12} className="mr-1 inline align-[-1px]" />Completed
            </span>
          ) : null}
        </div>
        <code className="mt-2 block text-sm text-[var(--color-text-secondary)]">
          {task.httpMethod} {task.endpoint}
        </code>
      </div>

      {task.tier === 'T1' || task.tier === 'T2' ? (
        <div
          className={`rounded-md border px-4 py-3 text-sm text-[var(--color-text)] ${
            task.tier === 'T1'
              ? 'border-[var(--color-violet)]/40 bg-[var(--color-violet)]/10'
              : 'border-[var(--color-yellow)]/50 bg-[var(--color-yellow)]/15'
          }`}
        >
          <span className="mb-1 block text-[11px] font-semibold uppercase tracking-wide text-[var(--color-text-secondary)]">
            {task.tier === 'T1' ? 'Tier 1 · AI-accelerated' : 'Tier 2 · human-in-the-loop'}
          </span>
          {/* The self-contained, promptable task spec — paste-ready for an AI agent (module, layer,
              method, SDK detail, goal). Falls back to the generic tier framing if unset. */}
          <p className="leading-relaxed">
            {task.description
              ? task.description
              : task.tier === 'T1'
                ? 'Implement exactly one commercetools SDK call in the repository, then validate it and explain it back. AI accelerates the plumbing; you verify it.'
                : "You own the design decision and the logic here; AI assists — there's a defensible answer to reach and defend, not a single generated one."}
          </p>
        </div>
      ) : null}

      {task.tier === 'T2' && task.decisions && task.decisions.length > 0 ? (
        <Section title="Decisions you own">
          <ul className="space-y-2 rounded-lg border border-[var(--color-yellow)]/40 bg-[var(--color-yellow)]/10 p-4">
            {task.decisions.map((d, i) => (
              <li key={i} className="flex items-start gap-2 text-sm text-[var(--color-text-secondary)]">
                <ListChecks size={16} className="mt-0.5 shrink-0 text-[var(--color-yellow)]" />
                <span>{d}</span>
              </li>
            ))}
          </ul>
          <p className="mt-2 text-xs text-[var(--color-text-muted)]">
            Decide and be ready to defend each — this is the human-in-the-loop work AI can assist but not settle for you.
          </p>
        </Section>
      ) : null}

      {task.hint ? (
        <Section title="Learn more">
          {/* A short pointer to the relevant commercetools docs — explore, don't copy. The full
              promptable spec (with the SDK detail) lives in the tier block above. */}
          <div className="flex items-start gap-2 rounded-lg border border-[var(--color-brd)] bg-[var(--color-bg-card)] p-3">
            <Lightbulb size={16} className="mt-0.5 shrink-0 text-[var(--color-teal-light)]" />
            <span className="min-w-0 break-words text-sm text-[var(--color-text-secondary)]">{task.hint}</span>
          </div>
        </Section>
      ) : null}

      {params.length > 0 ? (
        <Section title="Path parameters">
          <div className="space-y-2">
            {params.map((p) => (
              <label key={p} className="flex items-center gap-3 text-sm">
                <span className="w-24 text-[var(--color-text-muted)]">{p}</span>
                <input
                  value={values[p] ?? ''}
                  onChange={(e) => setValues((v) => ({ ...v, [p]: e.target.value }))}
                  placeholder={`Enter ${p}…`}
                  className="flex-1 rounded-md border border-[var(--color-brd)] bg-[var(--color-bg-panel)] px-2 py-1.5 text-[var(--color-text)] placeholder:text-[var(--color-text-muted)]"
                />
              </label>
            ))}
          </div>
        </Section>
      ) : null}

      <Section title="Query parameters">
        <p className="mb-2 text-xs leading-relaxed text-[var(--color-text-muted)]">
          Sent as the <code>?query</code>. Catalog reads use these for localization and price selection —{' '}
          <code>locale</code> · <code>priceChannel</code> · <code>priceCountry</code> · <code>priceCurrency</code> ·{' '}
          <code>priceCustomerGroup</code>. Edit them and <em>Try It</em> again to watch the effect: blank the
          currency to see the price fall back to <code>—</code>, or change <code>locale</code> to switch the
          name &amp; slug. Defaults match what the storefront sends.
        </p>
        <div className="space-y-2">
          {query.map((row, i) => (
            <div key={i} className="flex items-center gap-2 text-sm">
              <input
                value={row.key}
                onChange={(e) => setParam(i, { key: e.target.value })}
                placeholder="param"
                className="w-40 rounded-md border border-[var(--color-brd)] bg-[var(--color-bg-panel)] px-2 py-1.5 font-mono text-[var(--color-text)] placeholder:text-[var(--color-text-muted)]"
              />
              <span className="text-[var(--color-text-muted)]">=</span>
              <input
                value={row.value}
                onChange={(e) => setParam(i, { value: e.target.value })}
                placeholder={PARAM_HINTS[row.key] ?? 'value'}
                className="flex-1 rounded-md border border-[var(--color-brd)] bg-[var(--color-bg-panel)] px-2 py-1.5 text-[var(--color-text)] placeholder:text-[var(--color-text-muted)]"
              />
              <button
                onClick={() => removeParam(i)}
                aria-label={`Remove ${row.key || 'parameter'}`}
                className="rounded-md p-1.5 text-[var(--color-text-muted)] hover:bg-[var(--color-bg-card)] hover:text-[var(--color-text)]"
              >
                <X size={14} />
              </button>
            </div>
          ))}
          {query.length === 0 ? (
            <p className="text-xs text-[var(--color-text-muted)]">No query parameters — add one below.</p>
          ) : null}
        </div>
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <button
            onClick={() => addParam()}
            className="rounded-md border border-[var(--color-brd-light)] px-2.5 py-1 text-xs font-medium hover:border-[var(--color-violet)]"
          >
            + Add parameter
          </button>
          {CONTEXT_PARAMS.filter((p) => !query.some((r) => r.key === p)).map((p) => (
            <button
              key={p}
              onClick={() => addParam(p)}
              className="rounded-full border border-[var(--color-brd)] px-2.5 py-1 font-mono text-xs text-[var(--color-text-secondary)] hover:border-[var(--color-teal)] hover:text-[var(--color-teal-light)]"
            >
              + {p}
            </button>
          ))}
        </div>
      </Section>

      {takesBody ? (
        <Section title="Request body">
          <p className="mb-2 text-xs leading-relaxed text-[var(--color-text-muted)]">
            Sent as the JSON request body (<code>content-type: application/json</code>). Session 4 is the
            first with write tasks — edit the fields (a real <code>sku</code>, a <code>bundleKey</code>, a
            promo <code>code</code>) and <em>Try It</em>. A cart must exist first — run <code>4.1</code> once.
          </p>
          <textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            rows={Math.min(12, Math.max(3, body.split('\n').length + 1))}
            spellCheck={false}
            placeholder="{ }"
            className="w-full rounded-md border border-[var(--color-brd)] bg-[var(--color-bg-panel)] px-3 py-2 font-mono text-xs text-[var(--color-text)] placeholder:text-[var(--color-text-muted)]"
          />
        </Section>
      ) : null}

      <div className="flex flex-wrap items-center gap-3">
        <button
          onClick={() => onTry(task, task.httpMethod, resolved, takesBody ? body : undefined)}
          disabled={loading || missing.length > 0}
          title={missing.length > 0 ? `Enter ${missing.join(', ')} first` : undefined}
          className="inline-flex items-center gap-2 rounded-md bg-[var(--color-violet)] px-4 py-2 font-medium text-white hover:bg-[var(--color-violet-light)] disabled:cursor-not-allowed disabled:opacity-60"
        >
          {loading ? 'Calling BFF…' : 'Try It'}
        </button>
        {missing.length > 0 ? (
          <span className="text-xs text-[var(--color-yellow)]">
<TriangleAlert size={12} className="mr-1 inline align-[-1px]" />Enter <code>{missing.join(', ')}</code> to call this endpoint.
          </span>
        ) : (
          <span className="text-xs text-[var(--color-text-muted)]">
            Sends <code style={{ color: mc }}>{task.httpMethod}</code> <code>{resolved}</code> to your BFF
          </span>
        )}
        <a
          href={`${storefrontUrl}/?focus=${encodeURIComponent(task.capability)}`}
          target="_blank"
          rel="noreferrer"
          className="ml-auto rounded-md border border-[var(--color-brd-light)] px-3 py-1.5 text-sm hover:border-[var(--color-violet)]"
        >
          View in Storefront ↗
        </a>
      </div>

      {!completed ? (
        <div className="flex items-start gap-3 rounded-lg border border-[var(--color-yellow)]/30 bg-[var(--color-yellow)]/10 p-4 text-sm">
          <Info size={16} className="mt-0.5 shrink-0 text-[var(--color-yellow)]" />
          <div className="text-[var(--color-text-secondary)]">
            <strong className="text-[var(--color-text)]">Not implemented yet.</strong> Implement the one
            infrastructure adapter method for this task, then <em>Try It</em> — a <code>2xx</code> marks it
            complete and unlocks <code>{task.capability}</code> in the storefront.
          </div>
        </div>
      ) : null}
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section>
      <h2 className="mb-2 text-xs font-semibold uppercase tracking-wide text-[var(--color-text-muted)]">{title}</h2>
      {children}
    </section>
  );
}
