'use client';

import { useTranslations } from 'next-intl';
import { ShoppingCart } from '@/components/ui/icons';
import { Link } from '@/i18n/routing';
import { LanguageSwitch } from './LanguageSwitch';
import { CurrencySwitch } from './CurrencySwitch';
import { StoreSelector } from './StoreSelector';
import { StoreInfoBar } from './StoreInfoBar';
import { AnnouncementBar } from './AnnouncementBar';
import { SearchBar } from './SearchBar';

export function Header() {
  const t = useTranslations();
  return (
    <header className="sticky top-0 z-20">
      <AnnouncementBar />
      <div className="border-b border-[var(--color-border)] bg-[var(--color-cream)]/95 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center gap-6 px-4 py-3">
          <Link href="/" className="font-display whitespace-nowrap text-xl font-semibold tracking-tight">
            {t('common.shopName')}
          </Link>
          <div className="hidden flex-1 md:block">
            <SearchBar />
          </div>
          <nav className="flex items-center gap-4 text-sm">
            <StoreSelector />
            <LanguageSwitch />
            <CurrencySwitch />
            <Link href="/account" className="hover:text-[var(--color-terra)]">
              {t('nav.account')}
            </Link>
            <Link href="/cart" className="font-medium hover:text-[var(--color-terra)]">
              <ShoppingCart size={16} className="mr-1.5 inline align-[-3px]" />{t('nav.cart')}
            </Link>
          </nav>
        </div>
      </div>
      <StoreInfoBar />
    </header>
  );
}
