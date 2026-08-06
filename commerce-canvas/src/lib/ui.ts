import { ModuleGroup, TaskItem } from '../api/bff';

/** Tier accent: T1 = ct Violet (AI-accelerated), T2 = ct Yellow (human-in-the-loop).
    Solid fills with fixed-contrast text so the badge reads on both the dark shell and the light pane. */
export function tierClasses(tier: string): string {
  if (tier === 'T1') return 'bg-[var(--color-violet)] text-white';
  if (tier === 'T2') return 'bg-[var(--color-yellow)] text-[#191741]';
  return 'bg-[var(--color-bg-card)] text-[var(--color-text-muted)]';
}

/** HTTP method accent colour. */
export function methodColor(method: string): string {
  switch (method) {
    case 'GET':
      return 'var(--color-teal)';
    case 'POST':
      return 'var(--color-violet-light)';
    case 'PUT':
    case 'PATCH':
      return 'var(--color-yellow)';
    case 'DELETE':
      return 'var(--color-red)';
    default:
      return 'var(--color-violet)';
  }
}

export function pct(completed: number, total: number): number {
  return total === 0 ? 0 : Math.round((completed / total) * 100);
}

/** Teal background with alpha proportional to progress (heat cells). */
export function shade(percent: number): string {
  const a = (percent / 100) * 0.7;
  return `rgba(11, 191, 191, ${a.toFixed(3)})`;
}

/** Amber background with alpha proportional to the value (approach-flag heat cells). */
export function amberShade(percent: number): string {
  const a = (percent / 100) * 0.8;
  return `rgba(255, 200, 6, ${a.toFixed(3)})`;
}

/** Status pill styling: 2xx = done, 501 = pending (not an error), other = error. */
export function statusTone(status: number): { label: string; cls: string } {
  if (status === 0) return { label: 'no response', cls: 'bg-[var(--color-bg-card)] text-[var(--color-text-muted)]' };
  if (status >= 200 && status < 300)
    return { label: `${status} OK`, cls: 'bg-[var(--color-teal)]/20 text-[var(--color-teal-light)]' };
  if (status === 501) return { label: '501 pending', cls: 'bg-[var(--color-yellow)]/20 text-[var(--color-yellow)]' };
  return { label: `${status}`, cls: 'bg-[var(--color-red)]/20 text-[var(--color-red)]' };
}

export function flattenTasks(modules: ModuleGroup[]): TaskItem[] {
  return modules.flatMap((m) => m.sessions.flatMap((s) => s.tasks));
}

/** The numeric part of a session label ("Session 2" → 2); unlabelled sessions sort last. */
export function sessionNumber(session: string): number {
  const m = /(\d+)/.exec(session ?? '');
  return m ? Number(m[1]) : Number.MAX_SAFE_INTEGER;
}

/** The canonical task reference "<session>.<taskNumber>", e.g. "2.1" — the id used across the docs. */
export function taskRef(task: TaskItem): string {
  const m = /(\d+)/.exec(task.session ?? '');
  return m ? `${m[1]}.${task.taskNumber}` : String(task.taskNumber);
}

export function orderedSessions(modules: ModuleGroup[]): string[] {
  const seen: string[] = [];
  for (const m of modules) for (const s of m.sessions) if (!seen.includes(s.session)) seen.push(s.session);
  return seen.sort((a, b) => sessionNumber(a) - sessionNumber(b));
}

/**
 * Group all tasks by session for the sidebar accordion, in logical order: sessions ascending
 * (Session 1 → 2 → 3) and, within each, tasks by their number (so 3.1 → 3.2 → 3.3 … regardless of
 * which module a task lives in — e.g. the in-store PDP 3.2 sits in `catalog` but still lands between
 * 3.1 and 3.3).
 */
export function groupBySession(modules: ModuleGroup[]): { session: string; tasks: TaskItem[] }[] {
  const map = new Map<string, TaskItem[]>();
  for (const t of flattenTasks(modules)) {
    if (!map.has(t.session)) map.set(t.session, []);
    map.get(t.session)!.push(t);
  }
  return [...map.entries()]
    .sort(([a], [b]) => sessionNumber(a) - sessionNumber(b))
    .map(([session, tasks]) => ({
      session,
      tasks: [...tasks].sort((x, y) => x.taskNumber - y.taskNumber),
    }));
}
