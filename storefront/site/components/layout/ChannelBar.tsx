'use client';

import { usePreferences } from '@/context/preferences-context';

/**
 * Horizontal distribution-channel selector, shown only when the active store exposes more than one
 * channel. The selected channel becomes the `priceChannel` for PLP/PDP price selection, so switching
 * channels re-fetches and shows that channel's price (falling back to the currency price when the
 * channel has none). A single-channel (or no-channel) store hides this bar.
 */
export function ChannelBar() {
  const { channels, channel, setChannel } = usePreferences();
  if (channels.length <= 1) return null;

  return (
    <div className="border-b border-[var(--color-border)] bg-[var(--color-cream-dark)]/40">
      <div className="mx-auto flex max-w-6xl items-center gap-2 overflow-x-auto px-4 py-1.5 text-xs">
        <span className="shrink-0 text-[var(--color-charcoal-light)]">Price channel:</span>
        {channels.map((c) => (
          <button
            key={c}
            onClick={() => setChannel(c)}
            className={`shrink-0 rounded-full border px-2.5 py-0.5 ${
              channel === c
                ? 'border-[var(--color-terra)] bg-[var(--color-terra)]/10 text-[var(--color-terra)]'
                : 'border-[var(--color-border)] bg-white text-[var(--color-charcoal)] hover:border-[var(--color-terra)]'
            }`}
          >
            {c}
          </button>
        ))}
      </div>
    </div>
  );
}
