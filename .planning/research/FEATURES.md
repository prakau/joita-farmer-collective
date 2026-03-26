# Feature Research

**Domain:** Offline-first MRV and payout operations for smallholder climate programs
**Researched:** 2026-03-26
**Confidence:** MEDIUM-HIGH

## Feature Landscape

### Table Stakes (Users Expect These)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Farmer onboarding with consent and payout details | Operators need a real farmer record before any field or payout action is trustworthy | MEDIUM | Must be lightweight enough for field teams and include clear status gates |
| Offline parcel mapping | Field work happens where connectivity is unreliable | HIGH | Local save, resumable sync, and area metadata are core behaviors |
| Live geotagged evidence capture | Trust depends on proving where and when activity occurred | HIGH | Camera-only capture and geofence validation are part of the product promise |
| Operator review queue and audit timeline | Programs need a human-verifiable approval path before paying or reporting outcomes | MEDIUM | Must unify parcel, evidence, and status history |
| Payout reconciliation | Verified activity is not enough if payout status is opaque or retry-unsafe | MEDIUM | PSP request IDs and webhooks are non-optional for finance trust |

### Differentiators (Competitive Advantage)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Tamper-evident event chain | Turns Joita from a data-collection app into a trust infrastructure product | HIGH | Strong investor and auditor story if implemented clearly |
| Parcel overlap and double-count protection | Addresses a real MRV risk in fragmented landholdings | HIGH | H3 + polygon geometry makes this scalable |
| Investor heatmap and drill-down audit view | Makes credibility legible during fundraising and partner demos | MEDIUM | Should show proof, not vanity dashboards |
| AgriStack-ready adapter layer | Shows India-scale thinking without forcing full integration on day one | MEDIUM | Good for roadmap credibility, likely v2 |
| Low-cost remote QA with satellite context | Strengthens trust while preserving unit economics | HIGH | Good follow-on capability after core field workflow works |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Build full sovereign issuance in v1 | Sounds ambitious and impressive to outsiders | Pulls attention away from proving trusted capture and payout operations | Prepare registry-ready records, defer full issuance engine |
| Support every payment rail immediately | Feels like "future-proofing" | Expands integration and compliance burden too early | Start with one or two PSP-backed payout paths |
| Let agents upload from gallery for convenience | Feels faster during demos | Undermines the trust model and increases fraud risk | Make live capture fast and humane instead |
| Launch as a generic carbon platform | Sounds bigger | Weakens the strongest wedge and confuses investor story | Lead with offline MRV + payout trust for Indian field programs |

## Feature Dependencies

```text
Farmer onboarding
  -> requires consent capture
  -> enables payout enrollment

Offline parcel mapping
  -> requires local replica storage
  -> enables replay-safe sync

Live evidence capture
  -> requires approved parcel geofence
  -> supports operator verification
  -> enables payout trigger

Append-only event chain
  -> supports audit timeline
  -> supports investor reporting

Heatmaps and investor reporting
  -> depend on validated parcel geometry + payout/evidence state
```

### Dependency Notes

- **Payouts require farmer onboarding and approval states:** payout logic must never precede verified identity, consent, and parcel linkage.
- **Evidence depends on approved parcel context:** geofence validation is meaningless if the parcel record is weak or duplicate.
- **Investor reporting depends on operator truth:** dashboards should be downstream of verified operational status, not parallel shadow logic.
- **Event chaining should begin before advanced dashboard work:** it is cheaper to design auditability in from the start than retrofit it later.

## MVP Definition

### Launch With (v1)

- [ ] Premium demo shell that clearly tells the Joita story
- [ ] Farmer onboarding, consent, and payout enrollment
- [ ] Offline parcel mapping with sync-safe uploads
- [ ] Live geotagged evidence capture with integrity checks
- [ ] Operator review, statusing, and payout trigger workflows
- [ ] Investor dashboard views tied to real verification state

### Add After Validation (v1.x)

- [ ] Multilingual field guidance and better agent assistance
- [ ] Stronger retry automation and scheduled reconciliation jobs
- [ ] Portfolio comparison views for multi-program operators

### Future Consideration (v2+)

- [ ] AgriStack or state-specific verification adapters
- [ ] Satellite-assisted anomaly scoring and canopy reviews
- [ ] Registry-ready issuance lot generation and export tooling

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Offline parcel mapping | HIGH | HIGH | P1 |
| Live evidence capture | HIGH | HIGH | P1 |
| Operator review queue | HIGH | MEDIUM | P1 |
| Farmer onboarding and payout setup | HIGH | MEDIUM | P1 |
| Payout reconciliation | HIGH | MEDIUM | P1 |
| Tamper-evident event chain | HIGH | HIGH | P1 |
| Investor heatmap and audit drill-down | HIGH | MEDIUM | P1 |
| AgriStack adapters | MEDIUM | HIGH | P2 |
| Satellite-assisted QA | MEDIUM | HIGH | P2 |
| Multi-country support | LOW | HIGH | P3 |

## Working Competitive Frame

| Feature | Generic field data app | Generic climate dashboard | Joita approach |
|---------|-------------------------|---------------------------|----------------|
| Offline data capture | Often present, but not trust-oriented | Often weak or absent | Core to product identity |
| Evidence integrity | Usually basic media upload | Often abstracted away | Live capture + metadata + validation |
| Payout linkage | Rare | Rare | Treated as a first-class outcome |
| Investor storytelling | Weak | Strong but often disconnected from field proof | Must show proof and payout in one narrative |

## Sources

- Internal deliverables: `deliverable_src/prototype_link_sheet.html`, `deliverable_src/er_diagram.html`, `deliverable_src/india_scale_matrix.html`, `deliverable_src/tech_stack_brief.html`
- Official stack references listed in `.planning/research/STACK.md`

---
*Feature research for: Joita*
*Researched: 2026-03-26*
