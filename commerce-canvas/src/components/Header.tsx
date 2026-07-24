import { ModuleGroup } from '../api/bff';
import { flattenTasks, pct } from '../lib/ui';
import { Search, ExternalLink, LayoutDashboard } from '../lib/icons';

export type View = 'participant' | 'trainer';

export function Header({
  view,
  onView,
  connected,
  modules,
  completed,
  participantName,
  projectName,
  storefrontUrl,
  focusCapability,
  inspectorOpen,
  onToggleInspector,
}: {
  view: View;
  onView: (v: View) => void;
  connected: boolean;
  modules: ModuleGroup[];
  completed: Set<string>;
  participantName?: string;
  /** Connected commercetools project (from Task 1.1); falls back to the course identity until then. */
  projectName?: string | null;
  storefrontUrl: string;
  focusCapability?: string | null;
  inspectorOpen?: boolean;
  onToggleInspector?: () => void;
}) {
  const all = flattenTasks(modules);
  const done = all.filter((t) => completed.has(t.id)).length;
  const percent = pct(done, all.length);
  const storefrontHref = focusCapability
    ? `${storefrontUrl}/?focus=${encodeURIComponent(focusCapability)}`
    : storefrontUrl;

  return (
    <header className="flex h-14 flex-shrink-0 items-center gap-6 border-b border-[var(--color-brd)] bg-[var(--color-bg-sidebar)] px-5">
      <div className="flex min-w-[200px] items-center gap-3">
        <span className="font-display text-base font-bold" style={{ fontFamily: 'var(--font-display)' }}>
          <span className="text-[var(--color-violet-light)]">commercetools</span>
          <span className="text-[var(--color-text-muted)]"> Training</span>
        </span>
        <span
          className="border-l border-[var(--color-brd)] pl-3 text-xs text-[var(--color-text-muted)]"
          title={projectName ? `Connected commercetools project: ${projectName}` : undefined}
        >
          {projectName ?? 'Lifestyle & Home Corp'}
        </span>
      </div>

      <div className="flex flex-1 items-center gap-3">
        <div className="h-1.5 w-full max-w-[360px] overflow-hidden rounded-full bg-[var(--color-bg-card)]">
          <div
            className="h-full rounded-full transition-all duration-500"
            style={{ width: `${percent}%`, background: 'linear-gradient(90deg, var(--color-violet), var(--color-teal))' }}
          />
        </div>
        <span className="whitespace-nowrap text-xs text-[var(--color-text-secondary)]">
          {done}/{all.length} tasks · {percent}%
        </span>
      </div>

      <div className="flex items-center gap-3">
        {view === 'participant' && onToggleInspector ? (
          <button
            onClick={onToggleInspector}
            className={`rounded-md border px-3 py-1.5 text-sm ${
              inspectorOpen
                ? 'border-[var(--color-violet)] bg-[var(--color-bg-active)]'
                : 'border-[var(--color-brd-light)] bg-[var(--color-bg-card)] hover:border-[var(--color-violet)]'
            }`}
            title="Toggle the HTTP Inspector"
          >
            <Search size={14} className="mr-1.5 inline align-[-2px]" />Inspector
          </button>
        ) : null}
        <a
          href={storefrontHref}
          target="_blank"
          rel="noreferrer"
          className="rounded-md border border-[var(--color-brd-light)] bg-[var(--color-bg-card)] px-3 py-1.5 text-sm hover:border-[var(--color-violet)]"
        >
          <ExternalLink size={14} className="mr-1.5 inline align-[-2px]" />View in Storefront
        </a>
        <div className="flex rounded-lg border border-[var(--color-brd)] bg-[var(--color-bg-panel)] p-0.5 text-sm">
          <TabButton active={view === 'participant'} onClick={() => onView('participant')}>
            Participant
          </TabButton>
          <TabButton active={view === 'trainer'} onClick={() => onView('trainer')}>
            <LayoutDashboard size={14} className="mr-1.5 inline align-[-2px]" />Trainer
          </TabButton>
        </div>
        <span className="flex items-center gap-2 text-xs text-[var(--color-text-muted)]">
          <span
            className={`h-2 w-2 rounded-full ${connected ? 'bg-[var(--color-teal)]' : 'bg-[var(--color-red)]'}`}
          />
          BFF {connected ? 'online' : 'offline'} · :8081
        </span>
        {view === 'participant' && participantName ? (
          <span className="hidden text-xs text-[var(--color-text-secondary)] lg:inline">{participantName}</span>
        ) : null}
      </div>
    </header>
  );
}

function TabButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      onClick={onClick}
      className={`rounded-md px-3 py-1 font-medium ${
        active ? 'bg-[var(--color-violet)] text-white' : 'text-[var(--color-text-secondary)]'
      }`}
    >
      {children}
    </button>
  );
}
