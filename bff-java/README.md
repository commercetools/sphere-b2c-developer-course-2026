# bff-java — the BFF you build

A layered ("hexagonal") Spring Boot **Backend-for-Frontend** for commercetools. This is where the
course tasks live: you implement one SDK call at a time, and the storefront lights up.

> **Sessions 1–3** — **Ignition** (Platform, SDK, Project & Stores), **The Catalogue** (Product &
> Category reads, with channel/country/currency price selection), and **Find It** (store-scoped discovery
> via the Product Search API over GraphQL — search, facets, PLP).

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
- `catalog` — **your Session-2 tasks**: products (PLP/PDP), categories, variants, bundles, slug routing.
- `product-discovery` — **your Session-3 tasks**: store-scoped Product Search over GraphQL — search,
  full-text, category subtree, facets, PLP orchestration, postFilter.
- `training` — task/progress tracking (powers Commerce Canvas). Don't edit.
- `app` — the aggregator; runs the full BFF on **:8081**.

## Setup
1. **`.env`** — copy `.env.example` → `.env` and fill in your API-client credentials.
2. **AI toolchain** — in your editor (Claude Code), install the commercetools plugin:
   ```
   /plugin marketplace add commercetools/commercetools-ai-plugins
   /plugin install commercetools@commercetools
   ```
   When the plugin loads, Claude asks to approve a new MCP server — choose **"Use this and all future
   MCP servers in this project."**
   Validate it: ask the Knowledge MCP to confirm an SDK call. **From now on, ground every commercetools
   call on the Knowledge MCP — never guess the API.**
   > Only the **Knowledge MCP** (`commercetools-developer`, HTTP, no credentials) is required — it's
   > what grounds your code. It connects automatically.

### Optional — enable the Commerce MCP
The plugin also ships a **Commerce MCP** that lets the AI *call* your project's APIs. It's **optional**
(not needed for Session 1) and needs your API-client credentials as environment variables — note the
names differ from `.env` (no `CTP_` prefix):

```bash
export CLIENT_ID=…  CLIENT_SECRET=…  PROJECT_KEY=…  AUTH_URL=…  API_URL=…
```
(map from your `.env`: `CTP_CLIENT_ID → CLIENT_ID`, `CTP_CLIENT_SECRET → CLIENT_SECRET`,
`CTP_PROJECT_KEY → PROJECT_KEY`, `CTP_AUTH_URL → AUTH_URL`, `CTP_API_URL → API_URL`), then **restart
Claude**. Heads-up: a GUI IDE may not see shell `export`s — launch the editor from a terminal that has
them, or set them in the environment your IDE inherits.

## Run
```bash
mvn -pl app spring-boot:run     # full BFF on http://localhost:8081
```
Health check: `GET http://localhost:8081/api/project` should return the project (the worked reference).

## Dev loop (fast — no reinstall per task)
This applies to **every task**. The app ships with **Spring Boot DevTools**, which auto-restarts (~1–2s)
when compiled classes change — so you never run `mvn clean install` after a task.

1. **Run via the `BffApplication` run configuration** in IntelliJ (the green ▶ on `BffApplication`) —
   *not* the Maven `spring-boot:run` goal. This puts every module's `target/classes` on the classpath
   as directories, so DevTools watches your edits across all modules.
2. **Edit** your repository method → **recompile** (⇧⌘F9 *Recompile*, or ⌘F9 *Build Project*) →
   DevTools restarts automatically → hit **Try It**.
3. **Hands-free:** *Settings → Build, Execution, Deployment → Compiler → "Build project automatically"*
   — then a save triggers the compile and restart, no manual build.

> Full `mvn clean install` is only needed when a **`pom.xml`/dependency** changes, or to clear a stale
> build. If you run `mvn spring-boot:run` from a plain terminal instead of the IntelliJ config, edits in
> a *sibling* module won't be picked up until you rebuild that module (`mvn install -pl <module>`).

## Working a task
- Each task = **one stubbed method** in an `infrastructure` repository (it throws → `501`).
- Implement only that method (grounded on the MCP); the `application`/`api`/`domain` layers are wired.
- Endpoint returns `200` → capability unlocks → the storefront section lights up (watch it in Commerce
  Canvas / the storefront).
- **Session-1 tasks:** list stores, get a store by key (clean 404), and resolve the active store /
  region with a safe fallback.
- **Session-2 tasks:** list products (PLP) + product by key (PDP) with price selection (channel +
  country + currency), list categories, browse-by-category subtree, variant matrix, bundle roll-up,
  and localized slug routing.
- **Session-3 tasks:** store-scoped Product Search (GraphQL `productsSearch`) returning hydrated cards,
  full-text (+fuzzy), category subtree, facets (colour + price range/slider), the composed PLP
  (sort + pagination), and postFilter (stable facet counts) — plus the configurable-facets stretch.

Stuck? The **`solution`** branch has the reference implementation — try it yourself first.
