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
- [ ] 07-01: Bootstrap Android foundation, offline domain storage, recovered USB protocol and unit tests
- [ ] 07-02: Build the complete English/Hindi JOITA field interface, mobile integrations and offline reports
- [ ] 07-03: Generate JOITA release signing, build APK/AAB, verify and document handoff
