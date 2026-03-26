# Project Research Summary

**Project:** Joita
**Domain:** Offline-first MRV, verification, and payout operations for Indian field programs
**Researched:** 2026-03-26
**Confidence:** MEDIUM-HIGH

## Executive Summary

Joita is strongest when framed as a trust infrastructure product, not a generic climate dashboard. The internal materials consistently point toward one sharp promise: a field agent can capture a real-world event under weak connectivity, Joita can prove that the event belongs to a specific farmer and parcel, and the system can carry that proof all the way through operator review, payout, and investor visibility.

The recommended approach is to build Joita as an offline-first mobile-plus-ops stack with a local replica on device, a geospatial system of record in Postgres/PostGIS, and an append-only audit layer that feeds both operator workflows and investor reporting. The largest risk is over-expanding the scope into registry, compliance, or platform ambition before the core proof loop feels trustworthy and polished.

## Key Findings

### Recommended Stack

Joita should use an Android-first React Native mobile app backed by WatermelonDB + SQLite for the edge experience, then sync into Supabase Postgres/PostGIS with short-lived validation and webhook logic in Edge Functions. H3 is a good derived spatial index for overlap detection and portfolio heatmaps, but full parcel geometry should remain canonical.

**Core technologies:**
- React Native: mobile runtime for field workflows and device features
- WatermelonDB + SQLite: offline edge replica and sync queue
- Supabase Postgres + PostGIS: canonical data, auth/storage integration, spatial querying
- Supabase Edge Functions: validation, idempotent ingest, payouts, and webhooks
- H3: derived geospatial index for overlap and heatmap workloads

### Expected Features

Joita's table stakes are farmer onboarding, offline parcel capture, trusted evidence collection, operator review, and payout reconciliation. Its differentiators are tamper-evident event history, spatial anti-double-counting, and a premium investor-facing drill-down that is visibly tied to real verification state.

**Must have (table stakes):**
- Farmer onboarding with consent and payout readiness
- Offline parcel mapping with safe sync behavior
- Live geotagged evidence capture and geofence validation
- Operator review queue and payout reconciliation

**Should have (competitive):**
- Tamper-evident event chain
- Investor heatmaps and proof-driven dashboard drill-down
- Stronger remote QA hooks for later milestones

**Defer (v2+):**
- AgriStack adapters
- Satellite-assisted anomaly scoring
- Registry issuance automation

### Architecture Approach

The right architecture is mobile-local-first, backend-canonical, and audit-projection-oriented. Field work should be stored locally first, synced into canonical parcel and evidence tables, then mirrored into append-only event history and dashboard-friendly read models.

**Major components:**
1. Agent mobile app - onboarding, parcel capture, evidence capture, sync UX
2. Sync and validation layer - idempotency, geofence checks, ingest rules, webhooks
3. Canonical geospatial data layer - farmers, parcels, evidence, payouts, H3 indices
4. Audit/event projection layer - immutable history and dashboard projections
5. Operator and investor surfaces - review queue, timelines, heatmaps, payout views

### Critical Pitfalls

1. **Designing for happy-path connectivity** - make local-first workflows non-negotiable
2. **Treating evidence as media upload** - live capture plus metadata integrity must be built in
3. **Retrofitting auditability late** - event history should exist before advanced dashboards
4. **Over-promising compliance** - keep regulatory language as design direction until reviewed
5. **Building the empire before the wedge** - prove one trusted loop before adding everything else

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Premium Demo Foundation
**Rationale:** The first visible milestone must tell the Joita story beautifully and coherently.
**Delivers:** Design system, demo narrative, seeded data, and clickable end-to-end flow.
**Addresses:** Demo and investor-readiness requirements.
**Avoids:** Scope sprawl by forcing one clean story.

### Phase 2: Farmer Identity, Consent, and Enrollment
**Rationale:** Payout and verification credibility start with a trustworthy farmer record.
**Delivers:** Onboarding workflow, tokenized identity linkage, consent status, payout setup.
**Uses:** Canonical farmer model and stateful enrollment UX.

### Phase 3: Offline Parcel Mapping and Sync Safety
**Rationale:** Parcel truth is a prerequisite for geofence validation, overlap checks, and reporting.
**Delivers:** Local parcel capture, sync-safe uploads, overlap prevention.
**Implements:** Local replica and spatial storage patterns.

### Phase 4: Evidence Integrity and Audit Chain
**Rationale:** Joita's trust wedge becomes real only when evidence is verifiable and history is immutable.
**Delivers:** Live capture, metadata/signature flow, geofence validation, append-only event recording.
**Avoids:** Fake-trust dashboards backed by weak proofs.

### Phase 5: Operator Verification and Payout Ops
**Rationale:** The operator console turns verified field proof into decisions and cash movement.
**Delivers:** Review queue, approvals, payout triggers, reconciliation-safe status handling.
**Uses:** Validation outputs, role-based access, and PSP webhook integrations.

### Phase 6: Investor Reporting and Trust Controls
**Rationale:** Once operational truth exists, Joita can present it in a compelling investor format.
**Delivers:** Heatmaps, audit drill-downs, residency and retention controls, presentation polish.
**Implements:** Reporting read models and demo-hardening.

### Phase Ordering Rationale

- Parcel and evidence phases come before dashboard richness because proof must precede presentation.
- Event-chain and approval-state work come before investor reporting so the dashboard is downstream of truth.
- Compliance-sensitive controls land before the investor-ready milestone is declared complete.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 3:** offline sync contract, map tooling, and geometry correction UX
- **Phase 4:** device-side capture integrity and signature strategy
- **Phase 5:** PSP workflow details, webhook security, and operator queue design
- **Phase 6:** data-residency boundaries, retention rules, and presentation-grade reporting

Phases with standard patterns:
- **Phase 1:** premium shell, prototype flow, seeded data, and design system work
- **Phase 2:** structured onboarding and consent-state workflow

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Core stack validated against official docs and aligns with internal deliverables |
| Features | MEDIUM-HIGH | Strong internal product signal, but still pre-user-validation |
| Architecture | MEDIUM | Good fit for the domain, but exact repo structure remains to be proven in implementation |
| Pitfalls | MEDIUM | Strong domain fit, though some compliance items still need external review |

**Overall confidence:** MEDIUM-HIGH

### Gaps to Address

- Exact mobile bootstrap path for native modules and offline tooling should be confirmed in Phase 1 planning.
- Legal review is still needed before making any hard compliance claims in investor or production materials.
- PSP selection and webhook details should be narrowed before Phase 5 execution.

## Sources

### Primary (HIGH confidence)
- [React Native environment setup](https://reactnative.dev/docs/environment-setup)
- [WatermelonDB sync frontend](https://watermelondb.dev/docs/Sync/Frontend)
- [WatermelonDB sync implementation](https://watermelondb.dev/docs/Implementation/SyncImpl)
- [Supabase Edge Functions docs](https://supabase.com/docs/guides/functions)
- [Supabase PostGIS docs](https://supabase.com/docs/guides/database/extensions/postgis)
- [Supabase extensions overview](https://supabase.com/docs/guides/database/extensions)
- [H3 introduction](https://h3geo.org/docs/)

### Secondary (MEDIUM confidence)
- `deliverable_src/er_diagram.html`
- `deliverable_src/tech_stack_brief.html`
- `deliverable_src/india_scale_matrix.html`
- `deliverable_src/prototype_link_sheet.html`

---
*Research completed: 2026-03-26*
*Ready for roadmap: yes*
