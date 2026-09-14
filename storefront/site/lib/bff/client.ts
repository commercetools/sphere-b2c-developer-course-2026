/**
 * The ONE BFF client module. Every piece of data in this storefront comes from the training
 * BFF through here — there are NO direct commercetools calls anywhere in the storefront, and it
 * holds no commercetools credentials (only the BFF does).
 *
 * Calls go to the same-origin Next proxy at /api/bff/* (see app/api/bff/[...path]/route.ts),
 * which forwards to the BFF (BFF_URL, default http://localhost:8081/api). Because the contract
 * is identical, this behaves the same whether the BFF behind it is the Java or the JS build.
 *
 * It NEVER throws on a non-2xx response — a 501 (unimplemented task) comes back as
 * `notImplemented: true` so features can degrade to a locked state instead of crashing.
 */
export interface BffResult<T> {
  status: number;
  ok: boolean;
  /** true when the BFF returned 501 — the backing task is not implemented yet */
  notImplemented: boolean;
  data: T | null;
}

/**
 * Build the same-origin proxy URL. Each path segment (which may include a user-controlled
 * slug/key) is individually encoded and the result is always a *relative* URL under /api/bff —
 * so it can never address another host. Any query string is preserved as-is (it cannot change
 * the host of a relative URL).
 */
function toProxyUrl(path: string): string {
  const [rawPath, query] = path.replace(/^\/+/, '').split('?');
  const segments = rawPath.split('/').filter(Boolean).map(encodeURIComponent);
  return `/api/bff/${segments.join('/')}${query ? `?${query}` : ''}`;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<BffResult<T>> {
  try {
    const res = await fetch(toProxyUrl(path), {
      method,
      headers: { accept: 'application/json', 'content-type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    let data: T | null = null;
    try {
      data = (await res.json()) as T;
    } catch {
      data = null;
    }
    return { status: res.status, ok: res.ok, notImplemented: res.status === 501, data };
  } catch {
    // BFF unreachable — treat as a soft failure, never crash the UI.
    return { status: 0, ok: false, notImplemented: false, data: null };
  }
}

export function bffGet<T>(path: string): Promise<BffResult<T>> {
  return request<T>('GET', path);
}

export function bffPost<T>(path: string, body?: unknown): Promise<BffResult<T>> {
  return request<T>('POST', path, body);
}

export function bffPatch<T>(path: string, body?: unknown): Promise<BffResult<T>> {
  return request<T>('PATCH', path, body);
}

export function bffDelete<T>(path: string): Promise<BffResult<T>> {
  return request<T>('DELETE', path);
}

/** SWR fetcher: returns the whole result so callers can inspect `notImplemented`. */
export const bffFetcher = <T>(path: string): Promise<BffResult<T>> => bffGet<T>(path);
