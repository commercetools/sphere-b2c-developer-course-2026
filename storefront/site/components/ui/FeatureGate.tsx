'use client';

import { useCapability } from '@/context/capabilities-context';
import { Unlock } from '@/components/ui/icons';
import { useUnlockDialog } from '@/context/unlock-dialog';

/**
 * Wraps a capability-gated feature. The real block always renders (feature components fall back to
 * sample content when the BFF has no data), so the storefront always looks like a real shop:
 *  - unlocked → the live feature, fully interactive
 *  - locked   → the block is dimmed + non-interactive with an "Unlock" button centered on it;
 *               clicking it opens the dialog (which session unlocks it, which task to implement)
 *
 * Carries id="cap-<capability>" so Commerce Canvas deep links (?focus=<capability>) can scroll here.
 */
export function FeatureGate({
  capability,
  title,
  children,
  className,
}: {
  capability: string;
  title?: string;
  children: React.ReactNode;
  className?: string;
}) {
  const { loading, unlocked, meta } = useCapability(capability);
  const { open } = useUnlockDialog();
  const showOverlay = !loading && !unlocked;

  return (
    <section id={`cap-${capability}`} data-capability={capability} className={`relative scroll-mt-28 ${className ?? ''}`}>
      <div className={showOverlay ? 'pointer-events-none select-none opacity-40' : ''} aria-hidden={showOverlay}>
        {children}
      </div>
      {showOverlay ? (
        <div className="absolute inset-0 z-10 grid place-items-center">
          <button
            onClick={() => open(meta)}
            className="flex items-center gap-2 rounded-full bg-white/95 px-4 py-2 text-sm font-medium text-[var(--color-charcoal)] shadow-lg ring-1 ring-[var(--color-border)] hover:ring-[var(--color-terra)]"
            title={`Unlocks in ${meta.session}${meta.sessionName ? ` — ${meta.sessionName}` : ''}`}
          >
            <Unlock size={14} />Unlock{title ? `: ${title}` : ''}
          </button>
        </div>
      ) : null}
    </section>
  );
}
