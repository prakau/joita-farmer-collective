# Pitfalls Research

**Domain:** Offline-first field verification and payout systems
**Researched:** 2026-03-26
**Confidence:** MEDIUM

## Critical Pitfalls

### Pitfall 1: Designing for happy-path connectivity

**What goes wrong:**
The product feels fine in the office or demo room, but field agents lose work, duplicate submissions, or stall when coverage drops.

**Why it happens:**
Teams often treat offline as a caching problem instead of a primary product condition.

**How to avoid:**
Make every field workflow local-first, explicit about sync state, and resumable without data loss.

**Warning signs:**
- Agent actions are blocked on fresh network requests
- Retry behavior is unclear
- The team talks about offline as a future optimization

**Phase to address:**
Phase 3

---

### Pitfall 2: Treating evidence capture as media upload

**What goes wrong:**
Photos exist, but they do not prove where, when, or how an event occurred.

**Why it happens:**
Generic app patterns optimize for convenience, not trust.

**How to avoid:**
Enforce live capture, extract metadata on device, record signatures and hashes, and validate evidence against approved parcel boundaries.

**Warning signs:**
- Gallery uploads appear in scope
- Evidence records are just file URLs plus free text
- There is no status gate between captured evidence and verified evidence

**Phase to address:**
Phase 4

---

### Pitfall 3: Retrofitting auditability late

**What goes wrong:**
The dashboard looks polished, but nobody can prove how a payout or approval state was reached.

**Why it happens:**
Teams default to mutable business tables and only think about audit history when investors or auditors ask for it.

**How to avoid:**
Design an append-only event layer and status history early, then project read models from that history.

**Warning signs:**
- "Updated_at" is the main history source
- Approval reasons are overwritten instead of appended
- Payout state transitions are not preserved

**Phase to address:**
Phase 4

---

### Pitfall 4: Over-promising compliance before product proof

**What goes wrong:**
The team claims production-grade regulatory posture in investor materials before it has validated the technical and legal controls.

**Why it happens:**
Compliance language is persuasive, and early-stage teams blur direction with completed capability.

**How to avoid:**
State compliance-sensitive items as architectural constraints, implement clear controls, and leave final legal sign-off outside product copy until reviewed.

**Warning signs:**
- Marketing copy says "fully compliant" without internal review
- Storage and access policies are undocumented
- Sensitive and non-sensitive data are mixed casually

**Phase to address:**
Phase 6

---

### Pitfall 5: Building an empire before proving the wedge

**What goes wrong:**
The roadmap balloons into registries, satellite science, every payment rail, and multi-country operations before Joita proves one trusted flow.

**Why it happens:**
Climate infrastructure problems are broad and naturally tempt platform thinking.

**How to avoid:**
Use the narrowest complete loop that demonstrates trust and payout value: one farmer, one parcel, one evidence event, one payout, one audit trail.

**Warning signs:**
- Phase 1 includes registry exports, partner adapters, and complex analytics
- MVP language includes multiple countries or methodologies
- Demo requirements multiply faster than operational requirements

**Phase to address:**
Phase 1

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Store only simplified polygons | Faster first implementation | Auditors cannot replay capture quality or raw trace behavior | Only if raw trace capture is already queued for the same milestone |
| Manual payout status updates | Quicker demo scripting | Breaks reconciliation trust and invites duplication | Only for a strictly non-production prototype with obvious labels |
| One shared dashboard role | Faster early auth | Blurs investor, operator, and admin permissions | Only before Phase 5 if no real data is exposed |
| Single giant sync payload schema | Faster initial implementation | Hard to evolve safely across app versions | Never if offline sync is core to the product |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| PSP webhooks | Trusting callbacks without idempotency or signature checks | Validate signatures, store provider IDs, and make state transitions retry-safe |
| Edge Functions | Disabling JWT verification broadly | Only skip JWT verification for specific webhook endpoints that truly need it |
| Watermelon sync | Treating sync as ad hoc CRUD endpoints | Align with explicit pull/push sync semantics and conflict handling |
| Spatial validation | Using only bounding boxes or naive distance checks | Use canonical geometry plus proper geofence and overlap validation |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Polygon-only overlap scans | Slow approvals and timeouts | Derive H3 or other indices and keep spatial indexes healthy | As parcel counts grow beyond small pilots |
| Full-resolution evidence everywhere | Slow dashboards and expensive storage egress | Store derivatives for routine review and keep originals for audit paths | As evidence volume becomes meaningful |
| Chatty record-by-record sync | Battery drain and unstable retries | Batch sync intelligently with checkpoints and idempotency keys | Even in modest rural rollouts |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Storing raw government identifiers in business tables | Severe privacy and operational risk | Use tokenized references and isolate sensitive data |
| Public or weakly protected evidence URLs | Evidence leakage and misuse | Use signed access paths and role-aware policies |
| Shared operator credentials | Weak accountability | Use role-based auth and individual accounts |
| Unsigned payout or callback handling | Fraud or duplicate state changes | Verify callback authenticity and persist every transition |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Long text-heavy onboarding forms | Field agents delay or skip clean enrollment | Use concise, status-driven steps with obvious progress |
| Complex map editing on low-end devices | Agent fatigue and bad parcel quality | Guide perimeter capture with pauses, accuracy hints, and simple correction paths |
| Audit dashboards with no clear story | Investors see complexity instead of trust | Make one clean narrative path from field event to payout |

## "Looks Done But Isn't" Checklist

- [ ] **Offline sync:** verify work survives app restart and low connectivity, not just airplane-mode demos
- [ ] **Evidence capture:** verify gallery upload is blocked and geofence failures are handled visibly
- [ ] **Payout flow:** verify webhook retries do not create duplicate disbursement states
- [ ] **Investor dashboard:** verify every card drills into real underlying timeline data
- [ ] **Compliance controls:** verify sensitive data boundaries exist in implementation, not just slides

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Weak offline design | HIGH | Refactor the mobile data layer around local truth and explicit sync contracts |
| Missing audit chain | HIGH | Introduce immutable events, backfill projections carefully, and review historical gaps |
| Weak evidence integrity | HIGH | Rework capture flow, metadata handling, and server validation before scaling trust claims |
| Scope sprawl | MEDIUM | Cut to the narrowest end-to-end loop and descoped milestone goals |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Happy-path connectivity | Phase 3 | Field flows survive poor signal and replay safely |
| Evidence as generic media | Phase 4 | Evidence records show geofence, metadata, and integrity state |
| Retrofitted auditability | Phase 4 | Status history is append-only and payout review is traceable |
| Over-promised compliance | Phase 6 | Access, storage, and retention controls are explicit and reviewable |
| Empire-building | Phase 1 | Phase scope stays centered on one trusted investor-demo loop |

## Sources

- Internal deliverables: `deliverable_src/er_diagram.html`, `deliverable_src/india_scale_matrix.html`, `deliverable_src/prototype_link_sheet.html`
- Official stack references listed in `.planning/research/STACK.md`

---
*Pitfalls research for: Joita*
*Researched: 2026-03-26*
