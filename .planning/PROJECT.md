# Joita

## What This Is

Joita is an offline-first climate MRV and payout operations platform for Indian field programs working with fragmented smallholder farms. It gives field agents a reliable Android workflow for mapping parcels, capturing tamper-evident biochar or tree evidence, and syncing that activity into an auditable system that operators and investors can trust.

The product is being shaped first as an investor-ready MVP: a polished end-to-end story that proves Joita can connect one farmer, one parcel, one verified field event, one payout, and one audit trail without depending on perfect connectivity or manual spreadsheets.

## Core Value

Every farmer-linked field event must be capturable offline, verifiable with evidence and geofence rules, and traceable all the way to payout and audit review.

## Requirements

### Validated

(None yet - ship to validate)

### Active

- [ ] Field teams can onboard a farmer with consent, tokenized identity linkage, and payout enrollment.
- [ ] Agents can map farm polygons offline, sync safely, and prevent duplicate or overlapping claims.
- [ ] Agents can capture live, geotagged, tamper-evident evidence tied to approved parcels.
- [ ] Operators can review parcel and evidence timelines, approve or reject events, and trigger reconciliation-safe payouts.
- [ ] Investors can understand Joita's value quickly through a premium product narrative, demo flow, and dashboard outputs.

### Out of Scope

- Direct NPCI or bank-core integration in v1 - PSP payout rails are faster and safer for a first deployable prototype.
- Multi-country expansion - Joita needs to prove the India workflow, compliance posture, and economics before broadening scope.
- Full registry issuance automation in v1 - the first milestone should prove trusted field operations before building a sovereign issuance layer.
- Survey-grade hardware workflows - the cost thesis depends on commodity Android devices and low-cost remote validation.

## Context

- The current repo contains investor and architecture deliverables rather than application code.
- Existing materials already define the strongest product wedge: offline capture, fraud-resistant evidence, auditable farmer payouts, and India-specific deployment constraints.
- The product story currently speaks more strongly to investors and auditors than to day-to-day field operators; the MVP should correct that by making the farmer and agent workflow feel concrete and humane.
- The most compelling demo flow is: farmer onboarding -> parcel mapping offline -> live evidence capture -> operator review -> payout trigger -> investor dashboard and audit drill-down.
- Regulatory and privacy assumptions from the current materials should be treated as design constraints for MVP planning, then validated with legal and security review before production rollout.

## Constraints

- **Market**: India-first deployment - payout, identity, and data-handling assumptions are tailored to Indian programs and infrastructure.
- **Connectivity**: Offline-first capture is mandatory - field workflows must survive weak or intermittent rural mobile networks.
- **Device**: Android-first, low-end handsets - the product cannot assume premium hardware or stable background execution budgets.
- **Privacy**: Raw government identifiers should not live in Joita business tables - tokenized references and logical separation are required.
- **Security**: Evidence must be tamper-evident - live capture, on-device metadata extraction, signatures, and server-side validation are central to trust.
- **Cost**: The platform should preserve the current thesis of low per-farm operating cost - architectural choices must support that.
- **Presentation**: The first milestone must be investor-ready - UX quality, narrative flow, and demo polish are first-class deliverables.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Start as a greenfield GSD project built from the existing deliverable pack | There is no implementation code yet, but there is enough product direction to initialize serious planning | - Pending |
| Treat Joita as an offline-first MRV + payout product, not a generic climate dashboard | The strongest wedge is trusted field capture tied to financial outcomes | - Pending |
| Use India-first constraints as design inputs, not final legal claims | This keeps planning grounded without overstating compliance before counsel review | - Pending |
| Optimize the first milestone for a premium investor demo and a buildable MVP at the same time | The project needs both fundraising credibility and a realistic implementation path | - Pending |
| Keep direct registry automation out of the first milestone | The proof point is trusted capture and payout operations, not full sovereign issuance from day one | - Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `$gsd-transition`):
1. Requirements invalidated? -> Move to Out of Scope with reason
2. Requirements validated? -> Move to Validated with phase reference
3. New requirements emerged? -> Add to Active
4. Decisions to log? -> Add to Key Decisions
5. "What This Is" still accurate? -> Update if drifted

**After each milestone** (via `$gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check -> still the right priority?
3. Audit Out of Scope -> reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-03-26 after initialization*
