# storefront — the store that lights up

A Next.js storefront that consumes **your BFF** (never commercetools directly). Each section is
**capability-gated**: it stays locked until you implement the task that unlocks it — so the store
literally comes alive as you build.

> **Sessions 1–5:** you'll light up the **store-info bar** and **region switcher** (S1), the **catalogue
> PLP/PDP** (S2), the **store-scoped, searchable, faceted PLP** — search box, filter rail, sort — that
> the region switch re-scopes (S3), the **cart page + drawer** — add to cart, the bundle as one unit, the
> Subscribe & Save line, promo, the order summary, the wishlist (S4) — and the **account page** — sign in /
> register, the merge notice, profile, address book, your pricing tier, password, email verification (S5).
> Later sessions unlock checkout and orders.

## Run
```bash
cd site
npm install
npm run dev          # http://localhost:3000
```
Point it at the BFF on **:8081** (default). Start `bff-java` first, then this.

## What you'll see
- Before you implement a task, its section shows a **locked / sample** state.
- Implement the task in `bff-java` → the capability unlocks → the section renders **real data**
  (including your own store, once you create it in Merchant Center).
