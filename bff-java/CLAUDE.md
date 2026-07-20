# Standing instructions — LHC Training BFF (Java / Spring Boot)

- This is a modular Spring Boot BFF for a commercetools training course.
- ALWAYS ground commercetools API/SDK questions on the `commercetools-knowledge` MCP: search
  the docs, fetch the Java SDK schema, and validate any GraphQL before writing code. Never
  guess field names, update-action names, or SDK method paths.
- commercetools conventions to respect: prefer `key` over `id`; money is centAmount +
  currencyCode; text is LocalizedString; every update requires the current `version`; filter
  with query predicates server-side, not in code.
- Java 17, Maven multi-module. Each domain module must be independently runnable.
- Each domain module is internally layered (hexagonal): api / application / domain / infrastructure.
  The `infrastructure` repository holds ONLY the commercetools SDK call and returns raw SDK types;
  the `application` service maps SDK types to your own `domain` models. SDK types are confined to
  `infrastructure` + `application` — `domain` and `api` stay SDK-free. In tasks, participants
  implement ONLY the SDK call in the repository (T1); the service holds the mapping and any logic (T2).
- Read credentials only from a gitignored .env via spring-dotenv. Never hardcode.
