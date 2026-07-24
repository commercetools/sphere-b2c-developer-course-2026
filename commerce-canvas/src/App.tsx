import { useEffect, useMemo, useRef, useState } from 'react';
import { TaskItem, getProgress, getProgressAll, getProject, getTasks, getTelemetryAll, tryEndpoint } from './api/bff';
import { usePoll } from './hooks/usePoll';
import { flattenTasks } from './lib/ui';
import { X } from './lib/icons';
import { Header, View } from './components/Header';
import { Sidebar } from './components/Sidebar';
import { TaskDetail } from './components/TaskDetail';
import { Inspector, InspectorState } from './components/Inspector';
import { TrainerDashboard } from './components/TrainerDashboard';
import { BffDown } from './components/BffDown';

export function App() {
  const [view, setView] = useState<View>('participant');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [expandedSession, setExpandedSession] = useState<string | null>(null);
  const [inspector, setInspector] = useState<InspectorState | null>(null);
  const [loading, setLoading] = useState(false);
  // Inspector is hidden by default; opens on demand (toggle) or automatically on Try It.
  const [inspectorOpen, setInspectorOpen] = useState(false);

  const tasksF = usePoll(getTasks, 8000);
  const projectF = usePoll(getProject, 8000);
  const progressF = usePoll(getProgress, 3000);
  const allF = usePoll(getProgressAll, 4000);
  const telemetryF = usePoll(getTelemetryAll, 4000);
  const storefrontUrl = import.meta.env.VITE_STOREFRONT_URL ?? 'http://localhost:3000';

  const completedRef = useRef<Set<string>>(new Set());
  const [completed, setCompleted] = useState<Set<string>>(new Set());
  const autoOpened = useRef(false);

  useEffect(() => {
    const ids = new Set<string>();
    const perTask = progressF.data?.data?.perTask;
    if (perTask) for (const [id, done] of Object.entries(perTask)) if (done) ids.add(id);
    const mods = tasksF.data?.data;
    if (mods) for (const m of mods) for (const s of m.sessions) for (const t of s.tasks) if (t.completed) ids.add(t.id);
    let changed = false;
    const next = new Set(completedRef.current);
    ids.forEach((id) => {
      if (!next.has(id)) {
        next.add(id);
        changed = true;
      }
    });
    if (changed) {
      completedRef.current = next;
      setCompleted(next);
    }
  }, [tasksF.data, progressF.data]);

  const modules = tasksF.data?.data ?? [];
  const tasks = useMemo(() => flattenTasks(modules), [modules]);

  useEffect(() => {
    if (autoOpened.current || tasks.length === 0) return;
    const firstIncomplete = tasks.find((t) => !completed.has(t.id)) ?? tasks[0];
    setExpandedSession(firstIncomplete.session);
    setSelectedId((cur) => cur ?? firstIncomplete.id);
    autoOpened.current = true;
  }, [tasks, completed]);

  const selected = tasks.find((t) => t.id === selectedId) ?? null;

  const onSelect = (task: TaskItem) => {
    setSelectedId(task.id);
    setExpandedSession(task.session);
  };

  async function handleTry(task: TaskItem, method: string, endpoint: string) {
    setLoading(true);
    setInspectorOpen(true);
    const result = await tryEndpoint(method, endpoint);
    setInspector({ title: task.title, method, endpoint, result });
    setLoading(false);
    if (result.ok) void progressF.refresh();
  }

  const tasksFetched = tasksF.data;
  const bffDown = !!tasksFetched && tasksFetched.status === 0;
  const connected = !!tasksFetched && tasksFetched.status !== 0;

  const pid = progressF.data?.data?.participantId;
  const participantName = allF.data?.data?.find((p) => p.participantId === pid)?.participantName ?? pid;

  // The connected project — available once Task 1.1 (GET /api/project) is implemented; drives the
  // header title (falls back to the course's default identity before then).
  const project = projectF.data?.ok ? projectF.data.data : null;
  const projectName = project?.name || project?.key || null;

  return (
    <div className="flex h-screen flex-col overflow-hidden">
      <Header
        view={view}
        onView={setView}
        connected={connected}
        modules={modules}
        completed={completed}
        participantName={participantName}
        projectName={projectName}
        storefrontUrl={storefrontUrl}
        focusCapability={selected?.capability ?? null}
        inspectorOpen={inspectorOpen}
        onToggleInspector={() => setInspectorOpen((o) => !o)}
      />

      {bffDown ? (
        <BffDown />
      ) : view === 'trainer' ? (
        <div className="min-h-0 flex-1 overflow-auto">
          <TrainerDashboard
            summaries={allF.data?.data ?? []}
            telemetry={telemetryF.data?.data ?? []}
            modules={modules}
          />
        </div>
      ) : (
        <div className="flex min-h-0 flex-1">
          <Sidebar
            modules={modules}
            completed={completed}
            selectedId={selectedId}
            onSelect={onSelect}
            expandedSession={expandedSession}
            onToggleSession={(s) => setExpandedSession(s || null)}
          />
          <main
            className="surface-light min-w-0 flex-1 overflow-auto"
            style={
              selected?.tier === 'T1'
                ? { backgroundImage: 'radial-gradient(130% 55% at 50% 0%, rgba(99, 89, 255, 0.12), transparent 70%)' }
                : selected?.tier === 'T2'
                  ? { backgroundImage: 'radial-gradient(130% 55% at 50% 0%, rgba(255, 200, 6, 0.18), transparent 70%)' }
                  : undefined
            }
          >
            {selected ? (
              <TaskDetail
                key={selected.id}
                task={selected}
                completed={completed.has(selected.id)}
                loading={loading}
                storefrontUrl={storefrontUrl}
                onTry={handleTry}
              />
            ) : (
              <div className="grid h-full place-items-center p-8 text-center text-[var(--color-text-muted)]">
                {tasksFetched ? 'Select a task from the sidebar to get started.' : 'Connecting…'}
              </div>
            )}
          </main>

          {inspectorOpen ? (
            <aside className="flex w-[440px] flex-shrink-0 flex-col border-l border-[var(--color-brd)] bg-[var(--color-bg-panel)]">
              <div className="flex items-center justify-between border-b border-[var(--color-brd)] px-4 py-2.5">
                <span className="text-sm font-medium">HTTP Inspector</span>
                <button
                  onClick={() => setInspectorOpen(false)}
                  className="text-[var(--color-text-muted)] hover:text-[var(--color-text)]"
                  aria-label="Close inspector"
                >
                  <X size={16} />
                </button>
              </div>
              <div className="min-h-0 flex-1 overflow-auto">
                <Inspector state={inspector} loading={loading} />
              </div>
            </aside>
          ) : null}
        </div>
      )}
    </div>
  );
}
