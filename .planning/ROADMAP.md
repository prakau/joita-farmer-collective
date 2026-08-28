# Roadmap: Joita

## Overview

Joita's first milestone should prove one premium, trustworthy loop from field action to financial outcome: onboard a farmer, map a parcel offline, capture tamper-evident evidence, approve it operationally, trigger payout, and show the resulting audit trail in an investor-grade dashboard. The roadmap intentionally builds trust and narrative together so the product becomes both buildable and compelling to show.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

- [ ] **Phase 1: Premium Demo Foundation** - Establish the product story, visual system, and polished click-through flow investors can understand instantly.
- [ ] **Phase 2: Farmer Identity and Consent Enrollment** - Build trustworthy onboarding, tokenized identity linkage, and payout readiness.
- [ ] **Phase 3: Offline Parcel Mapping and Sync Safety** - Make parcel capture reliable under weak connectivity and safe to sync.
- [ ] **Phase 4: Evidence Integrity and Audit Chain** - Turn live field proof into tamper-evident, geofence-validated verification records.
- [ ] **Phase 5: Operator Verification and Payout Ops** - Give operators the workflow to approve events and move money safely.
- [ ] **Phase 6: Investor Reporting and Trust Controls** - Translate operational truth into investor reporting, residency controls, and final polish.

## Phase Details

### Phase 1: Premium Demo Foundation
**Goal**: Deliver a high-class product shell and investor-ready happy path that makes Joita's value obvious in under five minutes.
**Depends on**: Nothing (first phase)
**Requirements**: [DEMO-01, DEMO-02, DEMO-03]
**UI hint**: yes
**Success Criteria** (what must be TRUE):
  1. Investor can click through a complete flow from field action to payout and audit view without dead ends.
  2. Agent and dashboard surfaces share a consistent premium visual language.
  3. Demo data and copy clearly communicate why Joita is different from a generic dashboard or form app.
**Plans**: 3 plans

Plans:
- [ ] 01-01: Define the premium product narrative, information architecture, and shared design tokens
- [ ] 01-02: Build the clickable demo shell for agent flow and dashboard drill-down
- [ ] 01-03: Prepare investor demo assets, seeded data, and Loom-ready walkthrough structure

### Phase 2: Farmer Identity and Consent Enrollment
**Goal**: Create a trustworthy farmer onboarding workflow with payout details, tokenized identity handling, and explicit consent state.
**Depends on**: Phase 1
**Requirements**: [IDEN-01, IDEN-02, IDEN-03]
**UI hint**: yes
**Success Criteria** (what must be TRUE):
  1. Field staff can create and review a farmer profile with the required enrollment data.
  2. Identity handling stores tokens or references instead of raw sensitive IDs.
  3. Consent state visibly gates downstream verification and payout actions.
**Plans**: 3 plans

Plans:
- [ ] 02-01: Model farmer, consent, and payout-readiness states across product surfaces
- [ ] 02-02: Build onboarding and profile management flows for field and operator use
- [ ] 02-03: Implement tokenized identity and consent guardrails in backend workflows

### Phase 3: Offline Parcel Mapping and Sync Safety
**Goal**: Make parcel capture reliable offline and defensible when synced into the canonical geospatial system.
**Depends on**: Phase 2
**Requirements**: [MAP-01, MAP-02, MAP-03]
**UI hint**: yes
**Success Criteria** (what must be TRUE):
  1. Agent can map a parcel without connectivity and keep work safely on-device.
  2. Synced parcels preserve area, boundary hash, and capture-quality metadata.
  3. Duplicate or overlapping parcel claims are detected before approval.
**Plans**: 3 plans

Plans:
- [ ] 03-01: Establish local replica data model and parcel capture state machine
- [ ] 03-02: Implement sync contracts, idempotency handling, and canonical parcel persistence
- [ ] 03-03: Add overlap detection, review statuses, and parcel-quality feedback loops

### Phase 4: Evidence Integrity and Audit Chain
**Goal**: Ensure evidence is live, geofenced, integrity-checked, and permanently traceable through an append-only history.
**Depends on**: Phase 3
**Requirements**: [EVID-01, EVID-02, EVID-03, VERI-02]
**UI hint**: yes
**Success Criteria** (what must be TRUE):
  1. Agent can capture live evidence that includes device-side integrity data.
  2. Backend validation rejects evidence that falls outside parcel or workflow rules.
  3. Every important workflow change is written to an immutable audit chain that supports later drill-down.
**Plans**: 3 plans

Plans:
- [ ] 04-01: Build live capture, metadata extraction, and camera-only guardrails
- [ ] 04-02: Implement geofence validation and evidence ingestion workflows
- [ ] 04-03: Introduce append-only events and timeline projections for verification history

### Phase 5: Operator Verification and Payout Ops
**Goal**: Turn verified field activity into operator decisions and payout movements with clear status history.
**Depends on**: Phase 4
**Requirements**: [VERI-01, VERI-03, PAY-01, PAY-02, COMP-02]
**UI hint**: yes
**Success Criteria** (what must be TRUE):
  1. Operators can review, approve, or reject events with visible rationale and history.
  2. Approved events can trigger payout requests without ambiguous or duplicate status changes.
  3. Dashboard roles meaningfully separate field, operator, admin, and investor access paths.
**Plans**: 3 plans

Plans:
- [ ] 05-01: Build the operator queue, review timeline, and approval controls
- [ ] 05-02: Implement payout request orchestration, webhook handling, and reconciliation state
- [ ] 05-03: Add role-aware access boundaries across mobile, ops, and investor surfaces

### Phase 6: Investor Reporting and Trust Controls
**Goal**: Present Joita's verified operational truth in an investor-grade dashboard while formalizing residency and retention controls.
**Depends on**: Phase 5
**Requirements**: [PAY-03, COMP-01, COMP-03]
**UI hint**: yes
**Success Criteria** (what must be TRUE):
  1. Investors can view parcel, evidence, payout, and portfolio status through polished reporting flows.
  2. Sensitive data boundaries and residency assumptions are explicit in the implementation.
  3. Evidence and trace retention controls align with the intended audit window and data minimization posture.
**Plans**: 2 plans

Plans:
- [ ] 06-01: Build investor heatmaps, drill-down views, and final presentation polish
- [ ] 06-02: Implement residency-aware data boundaries, retention controls, and trust hardening

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4 -> 5 -> 6

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Premium Demo Foundation | 0/3 | Not started | - |
| 2. Farmer Identity and Consent Enrollment | 0/3 | Not started | - |
| 3. Offline Parcel Mapping and Sync Safety | 0/3 | Not started | - |
| 4. Evidence Integrity and Audit Chain | 0/3 | Not started | - |
| 5. Operator Verification and Payout Ops | 0/3 | Not started | - |
| 6. Investor Reporting and Trust Controls | 0/2 | Not started | - |
| 7. JOITA BioSeed AI Soil Saathi Android app | 3/3 | Built — physical UAT pending | 2026-08-11 |

### Phase 7: Rebuild SoilDetector as JOITA BIOSEED AI bilingual Android app

**Goal:** Deliver a signed, installable, offline-first Android app named JOITA BioSeed AI – Soil Saathi with a completely new accessible interface, English/Hindi localization, India-specific soil workflows, and explicit camera/GPS/share integrations.
**Requirements**: TBD
**Depends on:** Phase 6
**Plans:** 3 plans

**UI hint**: yes
**Success Criteria** (what must be TRUE):
  1. A signed APK installs and launches on supported Android devices while the supplied reference APK remains unchanged.
  2. Users can switch between English and Hindi and complete farmer, field, soil observation, recommendation, history, and report workflows offline.
  3. USB-OTG soil probe, camera, GPS, phone/WhatsApp, connectivity, and sharing integrations are explicit, permission-safe, and degrade gracefully to manual/offline paths.
  4. The new UI follows the JOITA logo-derived India-first visual system with large touch targets and clear status feedback.

Plans:
- [x] 07-01: Bootstrap Android foundation, offline domain storage, recovered USB protocol and unit tests
- [x] 07-02: Build the complete English/Hindi JOITA field interface, mobile integrations and offline reports
- [x] 07-03: Generate JOITA release signing, build APK/AAB, verify and document handoff

### Phase 8: Build JOITA Climate FieldOS reporting platform

**Goal:** Deliver a polished mobile-first, zero-dependency JOITA Climate FieldOS prototype linking farmers, fields, sensor observations, interventions, evidence, expenditure, and grant reporting in one honest offline-capable committee-ready experience.
**Requirements**: [FIELDOS-01, FIELDOS-02, FIELDOS-03, FIELDOS-04, FIELDOS-05, FIELDOS-06, FIELDOS-07, FIELDOS-08]
**Depends on:** Phase 7
**Plans:** 4 plans

**Phase 8 Requirements:**
- **FIELDOS-01**: Navigate a branded role-aware responsive shell, switch English/Hindi, search globally, and access all first-screen actions.
- **FIELDOS-02**: Inspect a state-derived dashboard with targets, KPIs, map, alerts, milestones, evidence completion, and offline queue.
- **FIELDOS-03**: Persist farmer, plot, Baseline Visit, and seven-step Quick Visit records locally with consent and provenance.
- **FIELDOS-04**: Create linked Soil Sathi, evidence, demonstration, meeting/training, sample/lab, feedback, and follow-up records with honest capture states.
- **FIELDOS-05**: Use Kisan Saathi, FarmAssist, BioSynth batch, document, attendance, and prototype QR views against shared state.
- **FIELDOS-06**: Reconcile fixed budgets, expense/cash proof, and batch/sample/invoice-to-activity traces without fabricated links.
- **FIELDOS-07**: Present committee, presentation, and grant-report previews without unsupported claims, fake exports, or statutory certification.
- **FIELDOS-08**: Automated real-browser smoke coverage verifies navigation, forms, reload persistence, offline queue, traceability, committee restrictions, and 320px overflow.

**Success Criteria** (what must be TRUE):
  1. A field user completes every primary action offline and recovers saved work after reload.
  2. Management inspects Kisan Saathis, advisories, batches, trainings, documents, evidence, fixed grant budget, and linked traces.
  3. Committee/report modes show current data and explicit evidence/export limitations without unsupported claims.
  4. The zero-dependency build and repeated real-browser smoke suite pass, including mobile overflow and role-control assertions.

Plans:
- [x] 08-01-PLAN.md — Typed local state, bilingual shell, search, responsive system, and dashboard
- [x] 08-02-PLAN.md — Farmer, plot, Baseline/Quick Visit, sensor, evidence, meeting, demonstration, sample, and follow-up
- [x] 08-03-PLAN.md — Kisan Saathi, FarmAssist, batch, documents, QR, finance, and traceability
- [x] 08-04-PLAN.md — Reports, committee/presentation modes, and automated browser smoke coverage
