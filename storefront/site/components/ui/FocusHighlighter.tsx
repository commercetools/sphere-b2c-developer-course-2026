'use client';

import { useEffect } from 'react';
import { useSearchParams } from 'next/navigation';

/**
 * Honours a ?focus=<capability> query param so Commerce Canvas's "View in Storefront" button
 * lands on the exact feature a task unlocks — scrolled into view and briefly highlighted.
 */
export function FocusHighlighter() {
  const params = useSearchParams();
  const focus = params.get('focus');

  useEffect(() => {
    if (!focus) return;
    const el = document.getElementById(`cap-${focus}`);
    if (!el) return;
    // Wait a tick so gated content has rendered.
    const t = window.setTimeout(() => {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.classList.add('cap-focus-highlight');
      window.setTimeout(() => el.classList.remove('cap-focus-highlight'), 2600);
    }, 250);
    return () => window.clearTimeout(t);
  }, [focus]);

  return null;
}
