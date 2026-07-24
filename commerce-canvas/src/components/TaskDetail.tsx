import { useState } from 'react';
import { TaskItem, endpointParams, resolveEndpoint } from '../api/bff';
import { methodColor, tierClasses } from '../lib/ui';
import { Check, Lightbulb, TriangleAlert, Info, ListChecks } from '../lib/icons';

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
  onTry: (task: TaskItem, method: string, endpoint: string) => void;
}) {
  const params = endpointParams(task.endpoint);
  const [values, setValues] = useState<Record<string, string>>({});
  const resolved = resolveEndpoint(task.endpoint, values);
  const missing = params.filter((p) => !(values[p] ?? '').trim());
  const mc = methodColor(task.httpMethod);

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

      <div className="flex flex-wrap items-center gap-3">
        <button
          onClick={() => onTry(task, task.httpMethod, resolved)}
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
