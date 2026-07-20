import { Unplug } from '../lib/icons';

/** Shown when the BFF training API is unreachable — never an error, just a nudge. */
export function BffDown() {
  return (
    <div className="grid flex-1 place-items-center">
      <div className="max-w-md rounded-2xl border border-[var(--color-brd)] bg-[var(--color-bg-panel)] p-8 text-center">
        <div className="mb-3 flex justify-center text-[var(--color-text-muted)]"><Unplug size={32} /></div>
        <h2 className="text-lg font-semibold">Start your BFF</h2>
        <p className="mt-2 text-sm text-[var(--color-text-secondary)]">
          Commerce Canvas can’t reach the training API on <code>:8081</code>. Start either flavour and
          this connects automatically — no reload needed.
        </p>
        <div className="mt-4 space-y-2 text-left text-xs" style={{ fontFamily: 'var(--font-mono)' }}>
          <pre className="overflow-x-auto rounded-md bg-[var(--color-bg-base)] p-3 text-[var(--color-text-secondary)]">
            cd bff-java &amp;&amp; mvn spring-boot:run -pl app
          </pre>
          <pre className="overflow-x-auto rounded-md bg-[var(--color-bg-base)] p-3 text-[var(--color-text-secondary)]">
            cd bff-ts &amp;&amp; npm run start
          </pre>
        </div>
      </div>
    </div>
  );
}
