'use client';

import { useCapability } from '@/context/capabilities-context';
import { usePreferences } from '@/context/preferences-context';
import { countryLabel, languageLabel } from '@/lib/utils';

/**
 * Slim store-facts strip, gated on project.info. Kept lightweight (not a big locked card) so the
 * shop chrome stays clean; still carries id="cap-project.info" for Canvas deep-links.
 */
export function StoreInfoBar() {
  const { loading, unlocked } = useCapability('project.info');
  return (
    <div
      id="cap-project.info"
      data-capability="project.info"
      className="scroll-mt-28 border-b border-[var(--color-border)] bg-[var(--color-cream-dark)]/50"
    >
      <div className="mx-auto max-w-6xl px-4 py-1.5 text-xs text-[var(--color-charcoal-light)]">
        {loading ? (
          <span>&nbsp;</span>
        ) : unlocked ? (
          <StoreFacts />
        ) : (
          <span>
            Store details (currencies · languages · delivery) unlock in Session 1 —{' '}
            <code>project.info</code>
          </span>
        )}
      </div>
    </div>
  );
}

function StoreFacts() {
  const { storeName, currency, country, language, channel } = usePreferences();
  return (
    <div className="flex flex-wrap items-center gap-x-6 gap-y-1">
      {storeName ? <span className="font-medium text-[var(--color-charcoal)]">{storeName}</span> : null}
      <Fact label="Currency" value={currency} />
      <Fact label="Country" value={countryLabel(country)} />
      <Fact label="Language" value={languageLabel(language)} />
      {channel ? <Fact label="Channel" value={channel} /> : null}
    </div>
  );
}

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <span>
      {label}: <span className="text-[var(--color-charcoal)]">{value}</span>
    </span>
  );
}
