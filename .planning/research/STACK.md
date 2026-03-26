# Stack Research

**Domain:** Offline-first climate MRV, field evidence, and payout operations for Indian agriculture
**Researched:** 2026-03-26
**Confidence:** MEDIUM-HIGH

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| React Native | 0.84 docs track | Android-first field app for agents | Official React Native docs continue to position it as the core path for native mobile apps, and it fits Joita's need for camera, GPS, and native-device integrations. |
| WatermelonDB + SQLite | 0.27.x docs track | Offline edge database and sync queue on-device | WatermelonDB's sync model is built around local replica data, change tracking, and resilient pull/push synchronization, which matches Joita's rural offline requirements. |
| Supabase Postgres + PostGIS | Managed platform | Core system of record for auth, storage metadata, spatial tables, and dashboard read models | Supabase provides hosted Postgres with PostGIS support and integrated auth/storage, which reduces MVP infrastructure lift while preserving strong geospatial capability. |
| Supabase Edge Functions | Current docs track | Idempotent ingestion, validation, webhook handling, and payout orchestration | Official docs position Edge Functions for TypeScript HTTP endpoints, webhooks, and short-lived server-side logic, which is a strong fit for Joita's verification and payout workflows. |
| H3 | 4.x docs track | Spatial indexing for overlap detection and portfolio heatmaps | H3's official docs emphasize hierarchical geospatial indexing and joining disparate data sets, which suits parcel overlap scans and investor-facing map aggregation. |

### Supporting Libraries and Services

| Library / Service | Version | Purpose | When to Use |
|-------------------|---------|---------|-------------|
| Supabase Auth | Managed service | Operator and internal user authentication | Use for operator/admin access and role-aware dashboard policies. |
| Supabase Storage | Managed service | Evidence asset storage and controlled delivery | Use for image uploads, derivatives, and access-controlled audit retrieval. |
| Postgres RLS | Built-in | Role-based separation of field, ops, and investor views | Use once the operator dashboard and investor views exist. |
| `pgcrypto` / hashing utilities | Current platform support | Server-side hashing and token helpers | Use for deterministic integrity checks where DB-side support is useful. |
| `pg_cron` or scheduled worker pattern | Current platform support | Retry and reconciliation jobs | Use for payout reconciliation, retention cleanup, and periodic verification sweeps. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Supabase CLI | Local functions, schema, and environment workflows | Use from the start so schema and function work are reproducible. |
| Figma | Maintain premium product and investor-demo fidelity | Keep mobile and dashboard flows aligned with Phase 1 UI goals. |
| Chrome headless PDF rendering | Regenerate investor collateral from HTML deliverables | Already present in the repo via `deliverable_src/render_pdfs.sh`. |

## Installation

```bash
# Mobile foundation
npm install react-native @nozbe/watermelondb @supabase/supabase-js h3-js

# Server-side and shared TypeScript
npm install @supabase/supabase-js

# Note
# Validate exact package versions when Phase 1 implementation starts.
# React Native framework choice should be confirmed against native module needs.
```

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| WatermelonDB + SQLite | Online-only caching or thin local storage | Only if field connectivity is reliably strong, which is not Joita's target reality. |
| Supabase Postgres + PostGIS | Firebase / Firestore | Use only if geospatial depth, SQL traceability, and database-controlled policies become less important than extreme realtime simplicity. |
| H3 indexing + polygon geometry | Polygon-only spatial checks | Use only for very small pilots; it becomes expensive and awkward as parcel counts and heatmap needs grow. |
| PSP payout rails | Direct bank/NPCI integration | Consider later if payout volume or economics justify deeper rails after MVP validation. |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Gallery-upload evidence as a normal path | It weakens trust and invites replay or fabricated proof | Camera-only live capture with metadata and signature checks |
| Cloud-only mobile state | Weak networks will create broken flows, lost work, and agent frustration | Local-first replica data with explicit sync checkpoints |
| Direct registry/issuance automation in MVP | It adds complexity before Joita proves trusted field operations | Verified event pipelines and operator-reviewed issuance preparation |
| Raw government ID storage in business tables | It creates unnecessary privacy and security risk | Tokenized references and logical data separation |

## Stack Patterns by Variant

**If Phase 1 needs maximum design speed for the investor prototype:**
- Use a design-system-first mobile and web shell with stubbed services.
- Because the first visible milestone must be presentation-grade without blocking on every backend detail.

**If the team prioritizes deployable field validation over visual breadth:**
- Build the agent mobile workflow and minimal operator console before advanced investor analytics.
- Because Joita's hardest problem is trusted capture and replay-safe sync, not dashboard chrome.

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| React Native docs track 0.84 | Native-camera, GPS, and SQLite integrations | Confirm exact library compatibility before implementation begins. |
| WatermelonDB 0.27.x docs track | SQLite-backed local replica workflows | Confirm project bootstrap path early because sync shape affects schema design. |
| Supabase Postgres + PostGIS | Spatial columns, GiST indexes, RPC functions | Supported path in Supabase docs; use dedicated schemas and indexes intentionally. |
| H3 4.x docs track | JS or service-side bindings | Best used as a derived index, not a replacement for canonical parcel geometry. |

## Sources

- [React Native environment setup](https://reactnative.dev/docs/environment-setup) - verified that React Native remains current and officially recommended for native mobile development
- [WatermelonDB sync frontend docs](https://watermelondb.dev/docs/Sync/Frontend) - verified the frontend synchronization model
- [WatermelonDB sync implementation docs](https://watermelondb.dev/docs/Implementation/SyncImpl) - verified conflict and pull/push sync behavior
- [Supabase Edge Functions docs](https://supabase.com/docs/guides/functions) - verified function runtime and webhook-oriented use cases
- [Supabase PostGIS docs](https://supabase.com/docs/guides/database/extensions/postgis) - verified PostGIS support inside Supabase
- [Supabase extensions overview](https://supabase.com/docs/guides/database/extensions) - verified extension model and available database capabilities
- [H3 introduction](https://h3geo.org/docs/) - verified H3 4.x positioning and indexing model
- Internal deliverables: `deliverable_src/er_diagram.html`, `deliverable_src/tech_stack_brief.html`, `deliverable_src/india_scale_matrix.html`

---
*Stack research for: Joita*
*Researched: 2026-03-26*
