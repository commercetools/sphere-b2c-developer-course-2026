import { NextIntlClientProvider, hasLocale } from 'next-intl';
import { getMessages } from 'next-intl/server';
import { notFound } from 'next/navigation';
import { Suspense } from 'react';
import type { Metadata } from 'next';
import { routing } from '@/i18n/routing';
import { CapabilitiesProvider } from '@/context/capabilities-context';
import { UnlockDialogProvider } from '@/context/unlock-dialog';
import { PreferencesProvider } from '@/context/preferences-context';
import { Header } from '@/components/layout/Header';
import { FocusHighlighter } from '@/components/ui/FocusHighlighter';
import '../globals.css';

export const metadata: Metadata = {
  title: 'Lifestyle & Home Corp',
  description: 'Training storefront — feature-gated on the LHC BFF',
};

export function generateStaticParams() {
  return routing.locales.map((locale) => ({ locale }));
}

export default async function LocaleLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  if (!hasLocale(routing.locales, locale)) notFound();
  const messages = await getMessages();

  return (
    <html lang={locale}>
      <body className="min-h-screen">
        <NextIntlClientProvider messages={messages}>
          <CapabilitiesProvider>
            <UnlockDialogProvider>
              <PreferencesProvider>
                <Suspense fallback={null}>
                  <FocusHighlighter />
                </Suspense>
                <Header />
                <main className="mx-auto max-w-6xl px-4 py-8">{children}</main>
              </PreferencesProvider>
            </UnlockDialogProvider>
          </CapabilitiesProvider>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
