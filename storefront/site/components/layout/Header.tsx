'use client';

import { useTranslations } from 'next-intl';
import { ShoppingCart, Heart } from '@/components/ui/icons';
import { Link } from '@/i18n/routing';
import { useCart } from '@/hooks/useCart';
import { useShoppingList } from '@/hooks/useShoppingList';
import { LanguageSwitch } from './LanguageSwitch';
import { CurrencySwitch } from './CurrencySwitch';
import { CountrySwitch } from './CountrySwitch';
import { ChannelBar } from './ChannelBar';
import { StoreSelector } from './StoreSelector';
import { StoreInfoBar } from './StoreInfoBar';
import { AnnouncementBar } from './AnnouncementBar';
import { SearchBar } from './SearchBar';

export function Header() {
  const t = useTranslations();
  const { cart } = useCart();
  const count = cart?.itemCount ?? 0;
  const { list } = useShoppingList();
  const saved = list?.itemCount ?? 0;
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
            <CountrySwitch />
            <CurrencySwitch />
            <Link href="/account" className="hover:text-[var(--color-terra)]">
              {t('nav.account')}
            </Link>
            {saved > 0 ? (
              <Link href="/cart" aria-label={`Saved items (${saved})`} title="Saved for later"
                className="inline-flex items-center gap-1 hover:text-[var(--color-terra)]">
                <Heart size={16} className="inline align-[-3px]" />
                <span className="text-xs font-semibold">{saved}</span>
              </Link>
            ) : null}
            <Link href="/cart" className="relative font-medium hover:text-[var(--color-terra)]">
              <ShoppingCart size={16} className="mr-1.5 inline align-[-3px]" />{t('nav.cart')}
              {count > 0 ? (
                <span className="ml-1.5 inline-flex min-w-[1.25rem] items-center justify-center rounded-full bg-[var(--color-terra)] px-1.5 py-0.5 text-[11px] font-semibold text-white">
                  {count}
                </span>
              ) : null}
            </Link>
          </nav>
        </div>
      </div>
      <ChannelBar />
      <StoreInfoBar />
    </header>
  );
}
