import { TryResult } from '../api/bff';
import { statusTone } from '../lib/ui';

export interface InspectorState {
  title: string;
  method: string;
  endpoint: string;
  result: TryResult;
}

/** HTTP Inspector: the last "Try It" call — method + path, status pill, and the JSON body. */
export function Inspector({ state, loading }: { state: InspectorState | null; loading: boolean }) {
  if (loading) {
    return <div className="p-4 text-sm text-[var(--color-text-muted)]">Calling BFF…</div>;
  }
  if (!state) {
    return (
      <div className="p-4 text-sm text-[var(--color-text-muted)]">
        Hit <span className="font-medium text-[var(--color-text-secondary)]">Try It</span> on a task to
        see the live request &amp; response here.
      </div>
    );
  }
  const tone = statusTone(state.result.status);
  return (
    <div className="space-y-3 p-4">
      <div className="flex items-center gap-2">
        <code className="text-xs text-[var(--color-text-secondary)]">
          {state.method} {state.endpoint}
        </code>
        <span className={`ml-auto rounded-full px-2 py-0.5 text-xs font-medium ${tone.cls}`}>{tone.label}</span>
      </div>
      {state.result.status === 501 ? (
        <p className="rounded-md bg-[var(--color-yellow)]/10 px-3 py-2 text-xs text-[var(--color-yellow)]">
          Pending — implement this task’s infrastructure method, then Try It again.
        </p>
      ) : null}
      {state.result.status === 0 ? (
        <p className="rounded-md bg-[var(--color-bg-card)] px-3 py-2 text-xs text-[var(--color-text-muted)]">
          No response — is your BFF running on :8081?
        </p>
      ) : null}
      <pre
        className="max-h-[60vh] overflow-auto rounded-md border border-[var(--color-brd)] bg-[var(--color-bg-base)] p-3 text-xs leading-relaxed text-[var(--color-text-secondary)]"
        style={{ fontFamily: 'var(--font-mono)' }}
      >
        {typeof state.result.body === 'string'
          ? state.result.body
          : JSON.stringify(state.result.body, null, 2)}
      </pre>
    </div>
  );
}
