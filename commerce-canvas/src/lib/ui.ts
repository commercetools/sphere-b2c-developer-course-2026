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

export function orderedSessions(modules: ModuleGroup[]): string[] {
  const seen: string[] = [];
  for (const m of modules) for (const s of m.sessions) if (!seen.includes(s.session)) seen.push(s.session);
  return seen;
}

/** Group all tasks by session label (ordered by first appearance) for the sidebar accordion. */
export function groupBySession(modules: ModuleGroup[]): { session: string; tasks: TaskItem[] }[] {
  const order: string[] = [];
  const map = new Map<string, TaskItem[]>();
  for (const t of flattenTasks(modules)) {
    if (!map.has(t.session)) {
      map.set(t.session, []);
      order.push(t.session);
    }
    map.get(t.session)!.push(t);
  }
  return order.map((session) => ({ session, tasks: map.get(session)! }));
}
