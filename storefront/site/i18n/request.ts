import { getRequestConfig } from 'next-intl/server';
import { routing } from './routing';

export default getRequestConfig(async ({ requestLocale }) => {
  let locale = await requestLocale;
  if (!locale || !routing.locales.includes(locale as (typeof routing.locales)[number])) {
    locale = routing.defaultLocale;
  }
  // Load the locale's chrome catalog; fall back to the default locale's messages if a routable
  // locale has no catalog yet (product/category content is still localized by the BFF per locale).
  let messages;
  try {
    messages = (await import(`../messages/${locale}.json`)).default;
  } catch {
    messages = (await import(`../messages/${routing.defaultLocale}.json`)).default;
  }
  return { locale, messages };
});
