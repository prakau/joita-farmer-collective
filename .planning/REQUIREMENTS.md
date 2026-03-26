# Requirements: Joita

**Defined:** 2026-03-26
**Core Value:** Every farmer-linked field event must be capturable offline, verifiable with evidence and geofence rules, and traceable all the way to payout and audit review.

## v1 Requirements

### Demo Experience

- [ ] **DEMO-01**: Investor can follow a coherent click-through from field visit to payout and audit trail without dead ends.
- [ ] **DEMO-02**: Mobile agent screens and operator dashboards share a polished visual system suitable for investor demos.
- [ ] **DEMO-03**: Demo data clearly shows one farmer, one parcel, one verified field event, one payout, and one audit timeline.

### Identity & Consent

- [ ] **IDEN-01**: Field staff can create a farmer profile with language preference, contact number, and payout details.
- [ ] **IDEN-02**: The platform stores tokenized government identity references instead of raw Aadhaar data.
- [ ] **IDEN-03**: Farmer consent status and consent artifacts are recorded before verification or payout workflows proceed.

### Parcel Mapping

- [ ] **MAP-01**: Agent can capture a farm perimeter offline on Android and save it locally until sync is possible.
- [ ] **MAP-02**: Synced parcel data includes boundary hash, calculated area, and survey accuracy metadata.
- [ ] **MAP-03**: The platform blocks duplicate or overlapping parcel claims before approval.

### Evidence Integrity

- [ ] **EVID-01**: Agent can capture live geotagged photo evidence from the camera only, not the gallery.
- [ ] **EVID-02**: The app extracts EXIF, GPS, timestamp, and integrity data on-device before upload.
- [ ] **EVID-03**: Server validation confirms evidence was captured inside the approved parcel geofence before it can support a ledger event.

### Verification & Operations

- [ ] **VERI-01**: Operators can review parcel, evidence, and ledger state in one timeline-oriented workflow.
- [ ] **VERI-02**: All farmer, parcel, evidence, approval, and payout actions are written to an append-only event chain with tamper detection.
- [ ] **VERI-03**: Operators can approve or reject field events with explicit status history and reason codes.

### Payouts & Reporting

- [ ] **PAY-01**: Verified events can trigger a farmer payout request through a PSP-backed workflow.
- [ ] **PAY-02**: Payout reconciliation stores provider request IDs, webhook outcomes, and retry-safe status transitions.
- [ ] **PAY-03**: Operators and investors can view parcel status, evidence coverage, payout status, and portfolio heatmaps.

### Compliance & Governance

- [ ] **COMP-01**: PII and payout data stay within India-resident storage boundaries separated logically from MRV evidence.
- [ ] **COMP-02**: Role-based access separates field, operator, admin, and investor views.
- [ ] **COMP-03**: Raw traces and evidence assets follow explicit retention and audit-window controls.

## v2 Requirements

### Verification Expansion

- **VEXP-01**: Platform can consume state-specific AgriStack or parcel-reference adapters where available.
- **VEXP-02**: Remote QA can flag parcel or canopy anomalies using satellite context.
- **VEXP-03**: Operators can generate registry-ready issuance lots and exports from verified records.

### Product Expansion

- **PROD-01**: Field workflows support richer multilingual guidance and voice-assisted capture.
- **PROD-02**: Platform supports multi-program portfolio comparisons and benchmarking.
- **PROD-03**: Joita supports additional geographies after India workflow validation.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Direct NPCI integration | PSP-backed payout rails are faster and lower-risk for MVP |
| iOS-first mobile delivery | Joita's field reality is Android-first |
| Gallery-based evidence uploads | Undermines the trust model |
| Full sovereign issuance automation in v1 | Too much scope before trusted field proof is validated |
| Multi-country rollout | Weakens the India-first wedge and complicates compliance posture |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| DEMO-01 | Phase 1 | Pending |
| DEMO-02 | Phase 1 | Pending |
| DEMO-03 | Phase 1 | Pending |
| IDEN-01 | Phase 2 | Pending |
| IDEN-02 | Phase 2 | Pending |
| IDEN-03 | Phase 2 | Pending |
| MAP-01 | Phase 3 | Pending |
| MAP-02 | Phase 3 | Pending |
| MAP-03 | Phase 3 | Pending |
| EVID-01 | Phase 4 | Pending |
| EVID-02 | Phase 4 | Pending |
| EVID-03 | Phase 4 | Pending |
| VERI-02 | Phase 4 | Pending |
| VERI-01 | Phase 5 | Pending |
| VERI-03 | Phase 5 | Pending |
| PAY-01 | Phase 5 | Pending |
| PAY-02 | Phase 5 | Pending |
| COMP-02 | Phase 5 | Pending |
| PAY-03 | Phase 6 | Pending |
| COMP-01 | Phase 6 | Pending |
| COMP-03 | Phase 6 | Pending |

**Coverage:**
- v1 requirements: 21 total
- Mapped to phases: 21
- Unmapped: 0

---
*Requirements defined: 2026-03-26*
*Last updated: 2026-03-26 after initial definition*
