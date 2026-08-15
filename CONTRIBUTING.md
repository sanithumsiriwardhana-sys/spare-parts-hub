# Contributing to spare-parts-hub

This doc exists so the "how we work" decisions we made don't only live in a chat log.
Read this before you branch off for your module.

## Stack

Spring Boot (Maven, Java 17) · Spring Data JPA · Spring Security · MySQL · Thymeleaf/Bootstrap

## Project structure

We use **package-by-feature**, not package-by-layer. Each of the 6 core functions gets its
own package, and each package owns its own `controller` / `service` / `repository`
subfolders and its own Thymeleaf templates folder. This means two people almost never edit
the same file, which is what actually prevents merge conflicts — branch naming alone
doesn't.

```
src/main/java/com/sliit/sparepartshub/
├── entity/            (shared — all 18 JPA entities + User, already committed)
├── repository/         (shared — UserRepository lives here; function-specific
│                        repositories live inside each function's own package)
├── security/           (shared — SecurityConfig, CustomUserDetailsService, etc.)
├── web/                 (shared — PageController: /, /login, /dashboard)
├── inventory/           (Function 1 — Inventory Storage Location Tracking)
├── sales/                (Function 2 — Product Search & Automated Checkout)
├── stockmonitoring/      (Function 3 — Dynamic Urgency Score Tracking)
├── warranty/             (Function 4 — Warranty and Returns Management)
├── supplier/             (Function 5 — Supplier Management)
└── reporting/            (Function 6 — Reporting, Audit Log & Supplier Portal)

src/main/resources/templates/
├── fragments/layout.html   (shared base layout — everyone's pages extend this)
├── login.html, dashboard.html   (shared)
└── inventory/, sales/, stockmonitoring/, warranty/, supplier/, reporting/
    (one folder per function, matching the packages above)
```

## Module ownership

| Function | Package | Actor(s) | Backlog items | Sprint |
|---|---|---|---|---|
| 1. Inventory Storage Location Tracking | `inventory` | Warehouse Clerk, Senior Sales Executive | PBI-01 to PBI-04 | Sprint 2 |
| 2. Product Search & Automated Checkout | `sales` | Senior Sales Executive | PBI-05 to PBI-08 | Sprint 1 |
| 3. Dynamic Urgency Score Tracking | `stockmonitoring` | Inventory Supervisor | PBI-09 to PBI-12 | Sprint 2/3 |
| 4. Warranty and Returns Management | `warranty` | Operations Coordinator | PBI-13 to PBI-16 | Sprint 3 |
| 5. Supplier Management | `supplier` | Shop Owner / Admin | PBI-17, PBI-18 | Sprint 3 |
| 6. Reporting, Audit Log & Supplier Portal | `reporting` | Admin (internal) + Supplier (external) | PBI-19, PBI-21 to PBI-24 | Sprint 4 |

_Fill in each teammate's name against their function below once assigned:_

- Inventory Storage Location Tracking — _name_
- Product Search & Automated Checkout — _name_
- Dynamic Urgency Score Tracking — _name_
- Warranty and Returns Management — _name_
- Supplier Management — _name_
- Reporting, Audit Log & Supplier Portal — _name_

**Shared tables, one owner:** `Product` is used by both Function 2 (search/checkout) and
Function 3 (urgency scoring). Whoever builds Function 2 first owns `ProductRepository` —
everyone else autowires it rather than creating a second one. If you need a new query
method on a repository you don't own, ask the owner to add it or open a small PR against
just that file.

## Branching

- One branch per member, named after their function:
  `feature/inventory-tracking`, `feature/pos-checkout`, `feature/urgency-tracking`,
  `feature/warranty-returns`, `feature/supplier-management`, `feature/reporting-portal`
- Branch off `main`, not off someone else's feature branch.
- Only touch your own package (`com.sliit.sparepartshub.<your-package>`) and your own
  templates folder. If a change outside your package is genuinely needed (e.g. adding a
  navbar link, extending a shared repository), say so in the PR description and tag the
  owner of that file.

## Local setup

1. Copy `application.properties.example` to `application.properties` (gitignored — never
   commit the real one) and fill in your local MySQL credentials.
2. Run the schema at `/database/schema.sql` against a local `spare_parts_db`.
3. Seed at least one test user per role so you can actually log in — see
   `docs/test-credentials.md` (or ask in the team chat) for ready-made BCrypt hashes.
4. `mvn clean compile` should succeed with zero errors before you write any new code.
5. `mvn spring-boot:run` should boot with no Hibernate schema-validation errors. This is
   the same bar we already hit with the `User` entity and the full 18-entity set — if it
   doesn't boot clean, fix that before adding your module.

## Building your module

1. Add any repositories your function needs under your own package
   (`com.sliit.sparepartshub.<yourpackage>.repository`), following the pattern in
   `UserRepository`.
2. Add a service layer if your logic is non-trivial (e.g. urgency score calculation,
   compatibility checking) — don't put business logic directly in the controller.
3. Replace the placeholder controller in your package (e.g. `InventoryController`) with
   real endpoints. The route prefix (`/inventory`, `/sales`, etc.) is already wired into
   `SecurityConfig` with the correct role restrictions — you shouldn't need to touch that
   file.
4. Replace the placeholder `index.html` in your templates folder. Extend the shared layout
   the same way it already does:
   ```html
   <html xmlns:th="http://www.thymeleaf.org"
         th:replace="~{fragments/layout :: page(title='Your Page Title', content=~{::main})}">
   <body>
   <main>
     <!-- your content -->
   </main>
   </body>
   </html>
   ```

## Merge criteria

Before opening a PR into `main`, your module must:

- [ ] Compile clean (`mvn clean compile`, zero errors)
- [ ] Boot clean (`mvn spring-boot:run`, no Hibernate validation errors)
- [ ] Be reachable end-to-end: log in as your role's test account, click your navbar link,
      confirm the page renders
- [ ] Not modify files outside your own package/templates folder, unless flagged and agreed
      with the owner of that file

Keeping `main` always in a demoable state matters — we have weekly checkpoints and a
Week 13 live demo, so nobody should be stuck untangling a broken `main` right before either.

## Known open decisions

- **Supplier authentication is separate from staff login.** `Supplier` is not in the
  `users` table (see `schema.sql` — the `role` ENUM only has the 5 internal roles), and it's
  intentionally excluded from the Staff actor generalization in our use case diagrams.
  Whoever builds Function 6 needs to design a separate login path for the external supplier
  portal — raise this with the team before starting, it's a real design decision, not just
  an implementation detail.
