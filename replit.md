# Workspace

## Overview

pnpm workspace monorepo using TypeScript. Each package manages its own dependencies.

## Stack

- **Monorepo tool**: pnpm workspaces
- **Node.js version**: 24
- **Package manager**: pnpm
- **TypeScript version**: 5.9
- **API framework**: Spring Boot
- **Database**: MySQL + Hibernate
- **Validation**: Hibernate
- **API codegen**: Swagger
- **Build**: Maven

## Key Commands

- `pnpm run typecheck` — full typecheck across all packages
- `pnpm run build` — typecheck + build all packages
- `pnpm --filter @workspace/api-spec run codegen` — regenerate API hooks and Zod schemas from OpenAPI spec
- `pnpm --filter @workspace/db run push` — push DB schema changes (dev only)
- `pnpm --filter @workspace/api-server run dev` — run API server locally

See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details.

## Medzoon Authentication & Dashboards

The `@workspace/medzoon` Angular 19 artifact includes a full client-side auth flow plus three role-specific dashboards.

- **Auth service** (`src/app/auth/auth.service.ts`) — signals-based, persists session in `localStorage` (`medzoon.session.v1`). Three demo accounts at `admin@medzoon.health`, `coord@medzoon.health`, `doctor@medzoon.health` (password `demo123`).
- **Roles**: `admin` (purple) → `/dashboard/admin`; `coordinatrice` (cyan) → `/dashboard/coordinatrice`; `doctor` (blue) → `/dashboard/doctor`.
- **Guards** (`src/app/auth/auth.guards.ts`): `authGuard` (must be signed in), `roleGuard(...roles)`, `dashboardRedirectGuard` (sends `/dashboard` to the user's role home).
- **Login** (`src/app/pages/login/login.component.*`) — split-screen form with demo-account quick-fill cards.
- **Dashboard shell** (`src/app/pages/dashboard/shared/dashboard-shell.component.*`) — sidebar + topbar (search, notifications, user menu with sign-out), role-aware nav.
- **Role pages** (`src/app/pages/dashboard/{admin,coordinatrice,doctor}/`) — each ships KPIs, tables, lists, and progress bars, sharing styles via `shared/dash-ui.scss`.
- The `AppComponent` hides the marketing navbar/footer on `/login` and `/dashboard/*` routes.

## Medzoon — Sub-pages, Drug Library & AI Assistant

- **Mock data** (`src/app/data/medical-data.ts`) — Employees, Consultations, Drugs (12 across 6 categories), Audit events, Schedule week.
- **Doctor sub-pages**: `/dashboard/doctor/{patients,consults,vaccines,drugs}` — patients list with filters, consultations log, vaccine coverage tracker, drug library with drag-and-drop prescription panel.
- **Coordinatrice sub-pages**: `/dashboard/coordinatrice/{employees,schedule,reminders}` — employee records (with create modal), weekly visit calendar, reminders + history.
- **Admin sub-pages**: `/dashboard/admin/{users,audit,settings}` — user/role management with invite modal, filterable audit log, organisation/security/retention settings.
- **PrescriptionService** (`pages/dashboard/doctor/prescription.service.ts`) — shared signal store consumed by both `DrugsComponent` and `AiAssistantComponent`.
- **AI Assistant** (`pages/dashboard/doctor/ai-assistant.component.*`) — floating bubble mounted globally for `doctor` role in the dashboard shell. Symptom-driven recommendations with penicillin allergy detection and "add to prescription" flow.
- **Sidebar nav** — extended in `dashboard-shell.component.ts` with all sub-routes per role.
