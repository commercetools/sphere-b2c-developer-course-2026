import { useMemo, useState } from 'react';
import { ModuleGroup, ParticipantSummary, ParticipantTelemetry, TaskSignal } from '../api/bff';
import { amberShade, flattenTasks, orderedSessions, pct, shade } from '../lib/ui';

type SortKey = 'name' | 'completion';

/** Trainer dashboard: participant × session table, class summary, per-task heat row, and the
    trainer-only "approach" signals (did they implement each task the expected way). */
export function TrainerDashboard({
  summaries,
  telemetry = [],
  modules,
}: {
  summaries: ParticipantSummary[];
  telemetry?: ParticipantTelemetry[];
  modules: ModuleGroup[];
}) {
  const sessions = orderedSessions(modules);
  const tasks = flattenTasks(modules);
  const [sortBy, setSortBy] = useState<SortKey>('completion');
  const [dir, setDir] = useState<1 | -1>(-1);

  // taskId -> title, for readable flag rows.
  const taskById = useMemo(() => new Map(tasks.map((t) => [t.id, t])), [tasks]);

  // Which tasks have an approach rule (someone has a signal for them) + per-task flagged count.
  const { checkedTasks, flaggedByTask, flagRows, totalFlags } = useMemo(() => {
    const checked = new Set<string>();
    const flaggedCount = new Map<string, number>();
    const rows: { participant: string; signal: TaskSignal }[] = [];
    let total = 0;
    for (const pt of telemetry) {
      for (const s of pt.signals) {
        checked.add(s.taskId);
        if (s.status === 'FLAGGED') {
          flaggedCount.set(s.taskId, (flaggedCount.get(s.taskId) ?? 0) + 1);
          rows.push({ participant: pt.participantName || pt.participantId, signal: s });
          total += 1;
        }
      }
    }
    return { checkedTasks: checked, flaggedByTask: flaggedCount, flagRows: rows, totalFlags: total };
  }, [telemetry]);

  const rows = useMemo(() => {
    const r = [...summaries];
    r.sort((a, b) => {
      const cmp =
        sortBy === 'name'
          ? a.participantName.localeCompare(b.participantName)
          : pct(a.completed, a.total) - pct(b.completed, b.total);
      return cmp * dir;
    });
    return r;
  }, [summaries, sortBy, dir]);

  if (summaries.length === 0) {
    return (
      <div className="mx-auto max-w-5xl p-6">
        <div className="rounded-xl border border-dashed border-[var(--color-brd)] bg-[var(--color-bg-panel)] p-8 text-center text-sm text-[var(--color-text-muted)]">
          No participant progress yet. As participants complete tasks, their rows appear here (from{' '}
          <code>GET /api/training/progress/all</code>).
        </div>
      </div>
    );
  }

  const classAvg = Math.round(summaries.reduce((s, p) => s + pct(p.completed, p.total), 0) / summaries.length);
  const fullyDone = summaries.filter((p) => p.total > 0 && p.completed === p.total).length;

  const sortHeader = (label: string, key: SortKey) => (
    <button
      onClick={() => {
        if (sortBy === key) setDir((d) => (d * -1) as 1 | -1);
        else {
          setSortBy(key);
          setDir(-1);
        }
      }}
      className="font-medium hover:text-[var(--color-violet-light)]"
    >
      {label}
      {sortBy === key ? (dir === 1 ? ' ▲' : ' ▼') : ''}
    </button>
  );

  return (
    <div className="mx-auto max-w-5xl space-y-5 p-6">
      <div className="grid grid-cols-3 gap-4">
        <Stat label="Participants" value={String(summaries.length)} />
        <Stat label="Class average" value={`${classAvg}%`} />
        <Stat label="Fully complete" value={`${fullyDone}/${summaries.length}`} />
      </div>

      <div className="overflow-x-auto rounded-xl border border-[var(--color-brd)] bg-[var(--color-bg-panel)]">
        <table className="w-full border-collapse text-sm">
          <thead>
            <tr className="border-b border-[var(--color-brd)] text-left text-[var(--color-text-secondary)]">
              <th className="px-3 py-2">{sortHeader('Participant', 'name')}</th>
              {sessions.map((s) => (
                <th key={s} className="px-3 py-2 text-center font-medium">
                  {s}
                </th>
              ))}
              <th className="px-3 py-2 text-right">{sortHeader('Total', 'completion')}</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((p) => (
              <tr key={p.participantId} className="border-b border-[var(--color-brd)]/60 last:border-0">
                <td className="px-3 py-2 font-medium">{p.participantName}</td>
                {sessions.map((s) => {
                  const c = p.perSession[s] ?? { completed: 0, total: 0 };
                  return (
                    <td key={s} className="px-3 py-2 text-center" style={{ backgroundColor: shade(pct(c.completed, c.total)) }}>
                      {c.total ? `${c.completed}/${c.total}` : '—'}
                    </td>
                  );
                })}
                <td className="px-3 py-2 text-right font-medium">{pct(p.completed, p.total)}%</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="overflow-x-auto rounded-xl border border-[var(--color-brd)] bg-[var(--color-bg-panel)]">
        <div className="px-3 py-2 text-xs font-semibold uppercase tracking-wide text-[var(--color-text-muted)]">
          Per-task completion — participants done
        </div>
        <table className="w-full border-collapse text-xs">
          <tbody>
            <tr>
              {tasks.map((t) => {
                const doneCount = summaries.filter((p) => p.perTask[t.id]).length;
                return (
                  <td
                    key={t.id}
                    title={`${t.title} — ${doneCount}/${summaries.length} done`}
                    className="border border-[var(--color-brd)] px-2 py-2 text-center"
                    style={{ backgroundColor: shade(pct(doneCount, summaries.length)) }}
                  >
                    <div className="font-medium">
                      {t.module.slice(0, 3)}·{t.taskNumber}
                    </div>
                    <div className="text-[10px] text-[var(--color-text-muted)]">
                      {doneCount}/{summaries.length}
                    </div>
                  </td>
                );
              })}
            </tr>
          </tbody>
        </table>
      </div>

      {checkedTasks.size > 0 ? (
        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-[var(--color-text-muted)]">
              Approach signals
            </h2>
            <span className="text-xs text-[var(--color-text-muted)]">
              how each task was implemented — not just whether it works
            </span>
            <span
              className={`ml-auto rounded-full px-2 py-0.5 text-xs font-medium ${
                totalFlags > 0
                  ? 'bg-[var(--color-yellow)]/20 text-[var(--color-yellow)]'
                  : 'bg-[var(--color-teal)]/20 text-[var(--color-teal-light)]'
              }`}
            >
              {totalFlags > 0 ? `${totalFlags} flag${totalFlags === 1 ? '' : 's'}` : 'all on the expected path'}
            </span>
          </div>

          {/* Per-task heat row: how many participants are flagged on each checked task. */}
          <div className="overflow-x-auto rounded-xl border border-[var(--color-brd)] bg-[var(--color-bg-panel)]">
            <div className="px-3 py-2 text-xs font-semibold uppercase tracking-wide text-[var(--color-text-muted)]">
              Per-task — participants flagged
            </div>
            <table className="w-full border-collapse text-xs">
              <tbody>
                <tr>
                  {tasks.map((t) => {
                    const checked = checkedTasks.has(t.id);
                    const flagged = flaggedByTask.get(t.id) ?? 0;
                    return (
                      <td
                        key={t.id}
                        title={
                          checked
                            ? `${t.title} — ${flagged}/${summaries.length} flagged`
                            : `${t.title} — no approach rule`
                        }
                        className="border border-[var(--color-brd)] px-2 py-2 text-center"
                        style={checked ? { backgroundColor: amberShade(pct(flagged, summaries.length)) } : undefined}
                      >
                        <div className="font-medium">
                          {t.module.slice(0, 3)}·{t.taskNumber}
                        </div>
                        <div className="text-[10px] text-[var(--color-text-muted)]">
                          {checked ? flagged : '—'}
                        </div>
                      </td>
                    );
                  })}
                </tr>
              </tbody>
            </table>
          </div>

          {/* Flag detail: participant · task · reasons — the actual "not as expected" conversations. */}
          {flagRows.length > 0 ? (
            <div className="overflow-hidden rounded-xl border border-[var(--color-brd)] bg-[var(--color-bg-panel)]">
              <div className="px-3 py-2 text-xs font-semibold uppercase tracking-wide text-[var(--color-text-muted)]">
                Flag detail
              </div>
              <ul className="divide-y divide-[var(--color-brd)]/60">
                {flagRows.map(({ participant, signal }, i) => {
                  const t = taskById.get(signal.taskId);
                  return (
                    <li key={`${signal.taskId}-${participant}-${i}`} className="px-3 py-2 text-xs">
                      <div className="flex flex-wrap items-baseline gap-x-2">
                        <span className="font-medium">{participant}</span>
                        <span className="text-[var(--color-text-muted)]">
                          {t ? `${t.title} (${t.module}·${t.taskNumber})` : signal.taskId}
                        </span>
                        <span className="ml-auto text-[10px] text-[var(--color-text-muted)]">
                          {signal.callCount} call{signal.callCount === 1 ? '' : 's'}
                        </span>
                      </div>
                      <div className="mt-1 flex flex-wrap gap-1">
                        {signal.flags.map((f, j) => (
                          <span
                            key={j}
                            className="rounded bg-[var(--color-yellow)]/15 px-1.5 py-0.5 font-mono text-[10px] text-[var(--color-yellow)]"
                          >
                            {f}
                          </span>
                        ))}
                      </div>
                      {signal.observed.length > 0 ? (
                        <div className="mt-1 font-mono text-[10px] text-[var(--color-text-muted)]">
                          {signal.observed.join('  ·  ')}
                        </div>
                      ) : null}
                    </li>
                  );
                })}
              </ul>
            </div>
          ) : (
            <div className="rounded-xl border border-dashed border-[var(--color-brd)] bg-[var(--color-bg-panel)] p-4 text-center text-xs text-[var(--color-text-muted)]">
              No approach flags — every attempted task used its expected commercetools calls.
            </div>
          )}
        </div>
      ) : null}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-[var(--color-brd)] bg-[var(--color-bg-panel)] p-4">
      <div className="text-2xl font-semibold">{value}</div>
      <div className="text-xs text-[var(--color-text-muted)]">{label}</div>
    </div>
  );
}
