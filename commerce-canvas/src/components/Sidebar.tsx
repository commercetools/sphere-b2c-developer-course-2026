import { ModuleGroup, TaskItem } from '../api/bff';
import { groupBySession, methodColor, tierClasses } from '../lib/ui';

/** Left panel: tasks grouped by session, accordion style. Click a task to open it in the main panel. */
export function Sidebar({
  modules,
  completed,
  selectedId,
  onSelect,
  expandedSession,
  onToggleSession,
}: {
  modules: ModuleGroup[];
  completed: Set<string>;
  selectedId: string | null;
  onSelect: (task: TaskItem) => void;
  expandedSession: string | null;
  onToggleSession: (session: string) => void;
}) {
  const groups = groupBySession(modules);

  return (
    <aside className="flex w-72 flex-shrink-0 flex-col overflow-y-auto border-r border-[var(--color-brd)] bg-[var(--color-bg-sidebar)]">
      <div className="px-4 py-3 text-xs font-semibold uppercase tracking-wide text-[var(--color-text-muted)]">
        Tasks
      </div>
      {groups.length === 0 ? (
        <div className="px-4 py-2 text-sm text-[var(--color-text-muted)]">Connecting to BFF…</div>
      ) : null}

      {groups.map(({ session, tasks }) => {
        const done = tasks.filter((t) => completed.has(t.id)).length;
        const isOpen = expandedSession === session;
        const allDone = done === tasks.length;
        return (
          <div key={session} className="border-b border-[var(--color-brd)]/60">
            <button
              onClick={() => onToggleSession(isOpen ? '' : session)}
              className={`flex w-full items-center justify-between px-4 py-2.5 text-left hover:bg-[var(--color-bg-hover)] ${
                isOpen ? 'bg-[var(--color-bg-panel)]' : ''
              }`}
            >
              <span className={`text-sm font-medium ${allDone ? 'text-[var(--color-teal-light)]' : ''}`}>
                {session}
              </span>
              <span className="flex items-center gap-2">
                <span className="rounded-full bg-[var(--color-bg-card)] px-2 py-0.5 text-[11px] text-[var(--color-text-secondary)]">
                  {done}/{tasks.length}
                </span>
                <span className={`text-[var(--color-text-muted)] transition-transform ${isOpen ? 'rotate-90' : ''}`}>
                  ›
                </span>
              </span>
            </button>

            {isOpen
              ? tasks.map((task) => {
                  const active = selectedId === task.id;
                  const isDone = completed.has(task.id);
                  return (
                    <button
                      key={task.id}
                      onClick={() => onSelect(task)}
                      className={`flex w-full items-center gap-2.5 px-4 py-2 text-left text-sm ${
                        active
                          ? 'bg-[var(--color-bg-active)]'
                          : task.tier === 'T1'
                            ? 'bg-[var(--color-violet)]/12 hover:bg-[var(--color-violet)]/20'
                            : task.tier === 'T2'
                              ? 'bg-[var(--color-yellow)]/12 hover:bg-[var(--color-yellow)]/20'
                              : 'hover:bg-[var(--color-bg-hover)]'
                      }`}
                    >
                      <span
                        className={`h-2 w-2 shrink-0 rounded-full ${
                          isDone ? 'bg-[var(--color-teal)]' : 'border border-[var(--color-brd-light)]'
                        }`}
                      />
                      <span className="min-w-0 flex-1 truncate">
                        <span className="text-[var(--color-text-muted)]">{task.module}·{task.taskNumber} </span>
                        {task.title}
                      </span>
                      <span className={`shrink-0 rounded px-1 py-0.5 text-[9px] font-semibold ${tierClasses(task.tier)}`}>
                        {task.tier}
                      </span>
                      <span
                        className="shrink-0 text-[10px] font-semibold"
                        style={{ color: methodColor(task.httpMethod) }}
                      >
                        {task.httpMethod}
                      </span>
                    </button>
                  );
                })
              : null}
          </div>
        );
      })}
    </aside>
  );
}
