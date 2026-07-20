'use client';

import { usePreferences } from '@/context/preferences-context';
import { languageLabel } from '@/lib/utils';

/** Language dropdown — options come from the active store (or project); changing it switches locale. */
export function LanguageSwitch() {
  const { languages, language, setLanguage } = usePreferences();
  // With a single language there's nothing to switch — show it as a static label.
  if (languages.length <= 1) {
    return <span className="text-sm text-[var(--color-charcoal-light)]">{languageLabel(language)}</span>;
  }
  return (
    <select
      aria-label="Language"
      value={language}
      onChange={(e) => setLanguage(e.target.value)}
      className="rounded-md border border-[var(--color-border)] bg-white px-2 py-1 text-sm"
    >
      {languages.map((l) => (
        <option key={l} value={l}>
          {languageLabel(l)}
        </option>
      ))}
    </select>
  );
}
