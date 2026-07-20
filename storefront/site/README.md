# LHC Storefront

The customer-facing **storefront** for *Lifestyle & Home Corp (LHC)* — a real Next.js 16 shop that
renders against **your BFF**, not commercetools directly. It holds **no commercetools credentials**:
every call goes through a same-origin proxy (`/api/bff/*`) to the BFF at `BFF_URL`.

Runs on **:3000**.

## How it works (capability-gated)

Storefront features are **gated on BFF capabilities**. The BFF reports which are unlocked
(`GET /api/training/capabilities`), and a capability unlocks the moment its backing task endpoint
returns **200** instead of 501. So:

- A **stubbed** BFF endpoint (501) shows its storefront section as a **locked feature** — dimmed, with
  an "Unlock" hint naming the task. It **never crashes** on a 501.
- Implement that task's infrastructure method in the BFF → the endpoint returns 200 → the section
  **lights up live**.
- Canvas's "View in Storefront" deep-links here via `?focus=<capability>` (scrolls to + highlights
  the matching section).

This is the demo payoff: implement one method in the BFF, watch the matching storefront feature
come alive.

## Prerequisites

- **Node 20.9+** (Next.js 16).
- A **running BFF** on `:8081` (either flavour — `../../bff-java/` or `../../bff-ts/`). The storefront
  runs on its own (locked features show sample data), but real data needs the BFF.

## Run

```bash
npm install
cp .env.example .env      # BFF_URL — where the proxy forwards (default http://localhost:8081/api)
npm run dev               # http://localhost:3000
```

Start a BFF on `:8081` first. Point at the other BFF flavour by changing `BFF_URL` — the storefront
is identical against Java or TypeScript.

## No credentials here

The only config is `BFF_URL` (server-side, used by `app/api/bff/[...path]/route.ts`). commercetools
credentials live **only** in the BFF's `.env`, never in the storefront.
