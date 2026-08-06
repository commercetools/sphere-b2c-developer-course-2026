# commercetools B2C Developer — Course Repo

Build a real storefront's backend — a **BFF** (Backend-for-Frontend) on commercetools — **method by
method, with AI as your co-pilot**. As you implement each task, a capability unlocks and a section of
the live **storefront** lights up.

> **This release covers Sessions 1–3 — "Ignition" (Platform, SDK, Project & Stores), "The Catalogue"
> (Product & Category reads), and "Find It" (store-scoped, faceted, searchable discovery via the Product
> Search API over GraphQL).** Later sessions add more modules to the same codebase.

## Branches

| Branch | What it is |
|---|---|
| **`main`** | **The starter — this is where you work.** Repository methods are stubbed (they return `501 Not Implemented`); you implement them. |
| **`solution`** | The reference answers. Peek only if you're stuck — try it yourself first. |

```bash
git clone <repo-url> && cd <repo>       # you're on main (the starter)
git switch solution                     # to see the answers
git switch main                         # back to your work
```

## What's inside

| Folder | What it is | You edit it? |
|---|---|---|
| [`bff-java/`](bff-java/) | The BFF you build — a layered ("hexagonal") Spring Boot service. | **Yes** — the tasks live here. |
| [`commerce-canvas/`](commerce-canvas/) | Your **progress dashboard + "Try It" console** — shows tasks, capabilities, and live requests. | No — run it. |
| [`storefront/`](storefront/) | The **live store** that lights up as your capabilities unlock. | No — run it. |

## Prerequisites
- **JDK 17** and **Maven** · **Node 18+** and **npm**
- A **commercetools project** + **Merchant Center** access *(provided for the course)*
- An **AI coding tool** (Claude Code or Codex) with the **commercetools plugin** *(installed in Session 1)*
- You've completed the **Platform Foundations** prerequisite.

## Setup (in order)
1. **Clone** this repo (you land on `main`, the starter).
2. **Create your API client** in Merchant Center (*Settings → Developer settings → API clients*). Name it with **your own name** — that's your identity in the progress tracker. Copy the credentials (shown once).
3. **Configure `.env`:** in `bff-java/`, copy `.env.example` to `.env` and paste your credentials.
4. **Install the AI toolchain:** in your editor, add the commercetools plugin (see [`bff-java/README.md`](bff-java/README.md)).

## Run it (three terminals)
```bash
# 1 · the BFF            → http://localhost:8081
cd bff-java && mvn -pl app spring-boot:run

# 2 · Commerce Canvas    → http://localhost:5173   (your dashboard + Try It)
cd commerce-canvas && npm install && npm run dev

# 3 · the Storefront     → http://localhost:3000   (lights up as you build)
cd storefront/site && npm install && npm run dev
```

## How you work through a task
1. Open **Commerce Canvas**, pick the next task.
2. Implement the **one stubbed method** in `bff-java` — **ground it on the Knowledge MCP; never guess the API.**
3. Hit **Try It** in Canvas → the endpoint returns `200` → the capability unlocks → the storefront section lights up.
4. **Explain it back** in your own words. Stuck? compare with the `solution` branch.

Per-project detail: [`bff-java/README.md`](bff-java/README.md) · [`commerce-canvas/README.md`](commerce-canvas/README.md) · [`storefront/README.md`](storefront/README.md).
