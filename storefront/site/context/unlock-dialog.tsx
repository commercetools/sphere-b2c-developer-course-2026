'use client';

import { createContext, useContext, useState } from 'react';
import { Lock } from '@/components/ui/icons';
import { CapabilityMeta, unlockHeadline } from '@/lib/capabilities';

interface UnlockDialogValue {
  open: (meta: CapabilityMeta) => void;
}

const UnlockDialogContext = createContext<UnlockDialogValue | null>(null);

/**
 * Provides the shared "feature locked" dialog. Any locked feature calls open(meta); the dialog
 * explains which session unlocks it and which task to implement. Mounted once at layout level.
 */
export function UnlockDialogProvider({ children }: { children: React.ReactNode }) {
  const [meta, setMeta] = useState<CapabilityMeta | null>(null);

  return (
    <UnlockDialogContext.Provider value={{ open: setMeta }}>
      {children}
      {meta ? (
        <div
          className="fixed inset-0 z-50 grid place-items-center bg-[var(--color-charcoal)]/40 p-4"
          onClick={(e) => {
            if (e.target === e.currentTarget) setMeta(null);
          }}
        >
          <div className="w-full max-w-md rounded-2xl border border-[var(--color-border)] bg-white p-6 shadow-xl">
            <div className="mb-1 text-[var(--color-charcoal)]"><Lock size={22} /></div>
            <h2 className="font-display text-xl">{unlockHeadline(meta)}</h2>
            <p className="mt-3 text-sm text-[var(--color-charcoal-light)]">
              This feature goes live once the task{' '}
              <span className="font-medium text-[var(--color-charcoal)]">{meta.taskLabel}</span> is
              implemented in the BFF.
            </p>
            {meta.bffEndpoint ? (
              <p className="mt-2 text-sm text-[var(--color-charcoal-light)]">
                Endpoint:{' '}
                <code className="rounded bg-[var(--color-cream-dark)] px-1.5 py-0.5 text-xs">
                  {meta.bffEndpoint}
                </code>
              </p>
            ) : null}
            <p className="mt-3 rounded-lg bg-[var(--color-cream-dark)] px-3 py-2 text-xs text-[var(--color-charcoal-light)]">
              Implement it in Commerce Canvas, hit <span className="font-medium">Try It</span>, and this
              section unlocks automatically — no reload.
            </p>
            <button
              onClick={() => setMeta(null)}
              className="mt-5 w-full rounded-lg bg-[var(--color-terra)] px-4 py-2.5 text-sm font-medium text-white hover:bg-[var(--color-terra-dark)]"
            >
              Got it
            </button>
          </div>
        </div>
      ) : null}
    </UnlockDialogContext.Provider>
  );
}

export function useUnlockDialog(): UnlockDialogValue {
  const ctx = useContext(UnlockDialogContext);
  if (!ctx) throw new Error('useUnlockDialog must be used within UnlockDialogProvider');
  return ctx;
}
