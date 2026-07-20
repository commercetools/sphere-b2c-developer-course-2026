// The ONE data source: the BFF training API. Same-origin '/api/*' is proxied to the BFF
// (Java or NestJS) by Vite, so there is no CORS and Canvas holds no credentials.

export interface TaskItem {
  id: string;
  module: string;
  session: string;
  taskNumber: number;
  title: string;
  tier: string;
  capability: string;
  endpoint: string;
  httpMethod: string;
  description?: string;
  hint?: string;
  completed: boolean;
}

export interface SessionGroup {
  session: string;
  tasks: TaskItem[];
}

export interface ModuleGroup {
  module: string;
  sessions: SessionGroup[];
}

export interface Progress {
  participantId: string;
  completed: number;
  total: number;
  perTask: Record<string, boolean>;
}

export interface Counts {
  completed: number;
  total: number;
}

export interface ParticipantSummary {
  participantId: string;
  participantName: string;
  completed: number;
  total: number;
  updatedAt: string;
  perSession: Record<string, Counts>;
  perTier: Record<string, Counts>;
  perTask: Record<string, boolean>;
}

/** Trainer-only: how a task was implemented, not just whether it works (approach telemetry). */
export interface TaskSignal {
  taskId: string;
  status: 'OK' | 'FLAGGED';
  flags: string[];
  callCount: number;
  observed: string[];
  checkedAt: string;
}

export interface ParticipantTelemetry {
  participantId: string;
  participantName: string;
  updatedAt: string;
  signals: TaskSignal[];
}

export interface Fetched<T> {
  status: number; // 0 = network error (BFF likely down)
  ok: boolean;
  data: T | null;
}

const BASE = '/api';

async function getJson<T>(path: string): Promise<Fetched<T>> {
  try {
    const res = await fetch(`${BASE}/${path.replace(/^\/+/, '')}`, {
      headers: { accept: 'application/json' },
    });
    let data: T | null = null;
    try {
      data = (await res.json()) as T;
    } catch {
      data = null;
    }
    return { status: res.status, ok: res.ok, data };
  } catch {
    return { status: 0, ok: false, data: null };
  }
}

export const getTasks = () => getJson<ModuleGroup[]>('training/tasks');
export const getProgress = () => getJson<Progress>('training/progress');
export const getProgressAll = () => getJson<ParticipantSummary[]>('training/progress/all');
export const getCapabilities = () => getJson<{ unlocked: string[] }>('training/capabilities');
/** Trainer-only approach signals for every participant (present when telemetry is enabled). */
export const getTelemetryAll = () => getJson<ParticipantTelemetry[]>('training/telemetry/all');

export interface TryResult {
  status: number; // 0 = network error
  ok: boolean;
  body: unknown;
}

/**
 * Fire a task's own endpoint (already includes /api) and capture status + body for the inspector.
 * The endpoint must be a *relative* same-origin `/api/...` path — never an absolute URL — so a
 * crafted "Try It" value can't turn this into a request to another origin. (Param values are also
 * encoded at {@link resolveEndpoint}.)
 */
export async function tryEndpoint(method: string, endpoint: string): Promise<TryResult> {
  if (!endpoint.startsWith('/api/') || endpoint.includes('//') || /^[a-z]+:/i.test(endpoint)) {
    return { status: 0, ok: false, body: 'Refused: endpoint must be a relative /api path' };
  }
  try {
    const res = await fetch(endpoint, { method, headers: { accept: 'application/json' } });
    const text = await res.text();
    let body: unknown = text;
    try {
      body = JSON.parse(text);
    } catch {
      /* keep as text */
    }
    return { status: res.status, ok: res.ok, body };
  } catch (e) {
    return { status: 0, ok: false, body: String(e) };
  }
}

/** Replace {param} placeholders in an endpoint with provided values (URL-encoded). */
export function resolveEndpoint(endpoint: string, params: Record<string, string>): string {
  // Encode each value so a user-supplied key/slug can't inject a new path segment, query, or
  // authority (e.g. "//evil" or "http://…") into the endpoint that tryEndpoint will fetch.
  return endpoint.replace(/\{([^}]+)\}/g, (_m, name: string) => encodeURIComponent(params[name] ?? ''));
}

/** The path-param names in an endpoint, e.g. "/api/products/{key}" -> ["key"]. */
export function endpointParams(endpoint: string): string[] {
  return [...endpoint.matchAll(/\{([^}]+)\}/g)].map((m) => m[1]);
}
