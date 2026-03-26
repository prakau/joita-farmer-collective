# Architecture Research

**Domain:** Offline-first climate MRV, verification, and payout workflows
**Researched:** 2026-03-26
**Confidence:** MEDIUM

## Standard Architecture

### System Overview

```text
[Agent Mobile App]
  onboarding | parcel mapping | evidence capture | sync status
    |
    v
[Local Replica: WatermelonDB + SQLite]
  farmers | parcels | evidence manifests | event queue | checkpoints
    |
    v  pull/push sync
[Sync + Validation API]
  auth checks | idempotency | geofence validation | webhooks
    |
    +--> [Postgres + PostGIS + H3]
    |      canonical farmers, parcels, payouts
    |
    +--> [Append-Only Event / Audit Layer]
           hash chain and projections

[Operator Console + Investor Dashboard]
  review queue | audit timeline | payout status | heatmaps
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| Agent mobile app | Capture field actions and preserve them under bad connectivity | React Native Android app with a local replica store |
| Local edge store | Hold authoritative pending work on device until sync succeeds | WatermelonDB + SQLite with explicit sync checkpoints |
| Sync and validation layer | Accept changes safely, reject duplicates, enforce geofence/integrity rules | Edge Functions or equivalent HTTP ingest layer |
| Canonical data layer | Persist farmers, parcels, evidence metadata, payout state, and queryable spatial data | Postgres + PostGIS + derived H3 indices |
| Audit/event layer | Preserve tamper-evident history and support downstream read models | Append-only event store with projections |
| Operator / investor surfaces | Turn verified operations into decisions and narratives | Web console with task views, timelines, and reporting |

## Recommended Project Structure

```text
apps/
  agent-mobile/    # React Native app for field agents
  ops-web/         # Operator and investor-facing web app
supabase/
  functions/       # Validation, sync, payouts, webhooks
  migrations/      # Database schema and policies
packages/
  domain/          # Shared business types and workflow rules
  ui/              # Shared design tokens and UI primitives
  maps/            # Geometry, H3, geofence utilities
  demo-data/       # Seed data and investor-demo fixtures
docs/
  investor/        # Demo script, screenshots, Loom notes
```

### Structure Rationale

- **`apps/agent-mobile/`:** Keeps the offline field workflow isolated from dashboard concerns.
- **`apps/ops-web/`:** Allows premium dashboard work without leaking web assumptions into the mobile app.
- **`supabase/functions/`:** Centralizes short-lived validation and webhook logic close to the data platform.
- **`packages/domain/`:** Prevents drift in status names, event types, and workflow rules across mobile and web surfaces.
- **`packages/demo-data/`:** Investor readiness improves when seeded data is deliberate, consistent, and reproducible.

## Architectural Patterns

### Pattern 1: Offline-First Local Replica

**What:** The mobile app treats on-device state as the working store and syncs to the backend opportunistically.
**When to use:** Always, because connectivity is a product constraint rather than an edge case.
**Trade-offs:** More sync complexity up front, but far better field reliability.

### Pattern 2: Append-Only Event Projection

**What:** Record important business actions as immutable events, then project them into dashboard-friendly read models.
**When to use:** For evidence, approvals, payouts, and status changes that must remain auditable.
**Trade-offs:** Extra modeling discipline, but much stronger trust and traceability.

### Pattern 3: Canonical Geometry + Derived Spatial Index

**What:** Store full parcel geometry as the source of truth and derive H3 cells or other indices for fast scans.
**When to use:** When you need both legal/audit fidelity and scalable overlap or heatmap queries.
**Trade-offs:** Slightly more storage and pipeline work, but better performance and explainability.

## Data Flow

### Key Data Flows

1. **Onboarding flow:** field agent creates or updates a farmer profile -> local store queues the change -> sync API validates fields -> canonical farmer record is created -> consent and payout readiness status are projected for the console.
2. **Parcel mapping flow:** agent captures perimeter walk -> local app stores raw trace, simplified polygon, and metadata -> sync API validates idempotency -> PostGIS stores geometry -> H3 cells are derived -> overlap checks determine approval state.
3. **Evidence flow:** live capture produces file hash, metadata, and signature -> sync API validates location and parcel status -> canonical evidence record is stored -> an append-only event is recorded -> operator review queue updates.
4. **Payout flow:** approved verification event triggers payout request -> PSP identifiers and webhook callbacks update payout state -> projections update operator and investor views.

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| 0-10k farms | Single managed Postgres + PostGIS instance, straightforward function layer, demo-focused projections |
| 10k-250k farms | Tighten spatial indexing, asset lifecycle management, and background reconciliation jobs |
| 250k+ farms | Separate heavy analytics from operational queries, consider async event pipelines for projections, and budget for richer observability |

### Scaling Priorities

1. **First bottleneck:** spatial validation queries and asset handling - solve with GiST indices, derived H3 cells, and disciplined media derivatives.
2. **Second bottleneck:** sync and reconciliation retries - solve with idempotency keys, durable checkpoints, and background job patterns.

## Anti-Patterns

### Anti-Pattern 1: Treating the dashboard as the product center

**What people do:** Build analytics and investor UI before the field workflow is trustworthy.
**Why it's wrong:** It produces beautiful surfaces backed by weak evidence.
**Do this instead:** Treat the field event chain as the product center and project dashboards from it.

### Anti-Pattern 2: Making geometry and audit history the same table concern

**What people do:** Cram mutable state and immutable audit records together.
**Why it's wrong:** It blurs the line between current state and provable history.
**Do this instead:** Keep canonical current-state tables and immutable event history as distinct but linked models.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| PSP payout provider | Webhook-backed request/response flow | Must be idempotent and retry-safe |
| Supabase Auth and Storage | Platform-native integration | Keep dashboard auth and media controls close to the data platform |
| Satellite or parcel-reference providers | Read-side verification input | Defer until the core field workflow is working |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Mobile app <-> sync layer | Authenticated HTTP pull/push sync | Keep payload contracts explicit and versioned |
| Sync layer <-> canonical data | Service functions + SQL/RPC | Validation should happen before projection side effects |
| Canonical data <-> dashboard | Read models / views | Dashboard queries should not own core business rules |

## Sources

- Internal deliverables: `deliverable_src/er_diagram.html`, `deliverable_src/tech_stack_brief.html`
- Official stack references listed in `.planning/research/STACK.md`

---
*Architecture research for: Joita*
*Researched: 2026-03-26*
