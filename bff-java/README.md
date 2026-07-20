# bff-java — the BFF you build

A layered ("hexagonal") Spring Boot **Backend-for-Frontend** for commercetools. This is where the
course tasks live: you implement one SDK call at a time, and the storefront lights up.

> **Session 1 — Ignition:** Platform, SDK, Project & Stores. (The `catalog` module arrives in
> Session 2.)

## The shape (four layers)
```
api             controllers + DTOs (the HTTP contract)
application     services — mapping + your logic
domain          your own models — SDK-free
infrastructure  the commercetools SDK call — the ONLY place SDK types live
```
**Golden rule:** SDK types never leak past `infrastructure`. The repository returns raw SDK types; the
service maps them to your `domain` models.

## Modules
- `platform` — shared kernel (the single `ProjectApiRoot` client, config, error advice). Don't edit.
- `project` — the worked reference (`GET /api/project`) **plus your Session-1 Stores tasks**.
- `training` — task/progress tracking (powers Commerce Canvas). Don't edit.
- `app` — the aggregator; runs the full BFF on **:8081**.

## Setup
1. **`.env`** — copy `.env.example` → `.env` and fill in your API-client credentials.
2. **AI toolchain** — in your editor (Claude Code), install the commercetools plugin:
   ```
   /plugin marketplace add commercetools/commercetools-ai-plugins
   /plugin install commercetools@commercetools
   ```
   Validate it: ask the Knowledge MCP to confirm an SDK call. **From now on, ground every commercetools
   call on the Knowledge MCP — never guess the API.**

## Run
```bash
mvn -pl app spring-boot:run     # full BFF on http://localhost:8081
```
Health check: `GET http://localhost:8081/api/project` should return the project (the worked reference).

## Working a task
- Each task = **one stubbed method** in an `infrastructure` repository (it throws → `501`).
- Implement only that method (grounded on the MCP); the `application`/`api`/`domain` layers are wired.
- Endpoint returns `200` → capability unlocks → the storefront section lights up (watch it in Commerce
  Canvas / the storefront).
- **Session-1 tasks:** list stores, get a store by key (clean 404), and resolve the active store /
  region with a safe fallback.

Stuck? The **`solution`** branch has the reference implementation — try it yourself first.
