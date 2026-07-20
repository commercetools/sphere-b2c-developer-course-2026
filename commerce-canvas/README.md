# commerce-canvas — your dashboard & "Try It" console

A small React/Vite app that is your **cockpit** for the course. It reads the BFF's training API and
shows:
- the **task list** (grouped by module & session) with your completion state,
- **capabilities** that unlock as you implement tasks,
- a **"Try It"** console to fire an endpoint and see the live request/response,
- a **Trainer** view (class progress) for the instructor.

You don't edit this — you run it and use it to drive your work.

## Run
```bash
npm install
npm run dev          # http://localhost:5173
```
It proxies `/api/*` to the BFF on **:8081**, so start `bff-java` first. No credentials live here.

## Using it
1. Pick the next task from the sidebar.
2. Implement its method in `bff-java`.
3. Hit **Try It** → a green `200` flips the task ✓ and unlocks its capability.
4. Switch to the **Storefront** to see the matching section light up.
