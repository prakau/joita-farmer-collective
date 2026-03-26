# Phase 1: Premium Demo Foundation - Context

**Gathered:** 2026-03-26
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase establishes the Joita product story, premium visual system, seeded demo data, and polished click-through flow that investors can understand instantly. It does not build the full production backend; it defines and demonstrates the experience contract that later phases will implement in depth.

</domain>

<decisions>
## Implementation Decisions

### Product narrative
- **D-01:** Phase 1 must tell one complete story: trusted field capture leads to farmer payout and investor confidence.
- **D-02:** Every major screen should stay anchored to a real entity or outcome: farmer, parcel, evidence, approval, payout, or audit proof.

### Visual language
- **D-03:** The product should look like premium trust infrastructure, not a generic climate dashboard or NGO form system.
- **D-04:** Use a warm earth-and-mineral palette with deep ink surfaces and a restrained copper accent.
- **D-05:** Avoid flat green-on-white sustainability cliches and avoid overly playful consumer-app styling.

### Mobile agent experience
- **D-06:** Agent screens should be guided, low-cognitive-load, and easy to operate on low-end Android devices.
- **D-07:** Mapping, capture, and sync states must be obvious and confident, with large primary actions and visible status.

### Operator and investor dashboard experience
- **D-08:** Dashboard screens should prioritize proof chain visibility: parcel -> evidence -> approval -> payout.
- **D-09:** Heatmaps and KPI cards should support the narrative, not dominate it or turn the product into a vanity analytics tool.

### Demo structure
- **D-10:** The default demo sequence is onboarding summary -> parcel mapping -> live evidence capture -> operator verification -> payout event -> investor drill-down.
- **D-11:** Seed data should revolve around one hero farmer and one hero parcel so the story is memorable.
- **D-12:** The phase should feel Loom-ready by the end, with a click path that can be narrated smoothly in under five minutes.

### Motion and polish
- **D-13:** Use intentional transitions and staged reveals to make the product feel premium.
- **D-14:** Motion should reinforce trust and flow, not feel playful, noisy, or social-media-like.

### the agent's Discretion
- Exact component APIs and file structure for the future implementation
- Charting and map rendering libraries
- Animation implementation details
- Whether Phase 1 uses stubbed or lightly connected data behind the demo shell

</decisions>

<specifics>
## Specific Ideas

- "High class" means premium, calm, and convincing, not crowded or flashy.
- The product should feel closer to Stripe-grade clarity and Linear-grade precision than to a generic ESG deck.
- The dashboard should make the investor think, "I can trust how this data was produced," not just "this looks modern."
- The mobile flow should feel humane for field teams: big actions, obvious progress, and no bureaucratic clutter.

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Product scope
- `.planning/PROJECT.md` - Product identity, core value, constraints, and scope boundaries
- `.planning/REQUIREMENTS.md` - Phase-mapped requirements for the investor-ready MVP
- `.planning/ROADMAP.md` - Phase 1 goal, success criteria, and downstream sequencing

### Existing investor and concept materials
- `deliverable_src/prototype_link_sheet.html` - Existing demo flow and investor handoff intent
- `deliverable_src/er_diagram.html` - Core trust architecture and proof-chain logic
- `deliverable_src/tech_stack_brief.html` - Stack and cost thesis for Joita
- `deliverable_src/india_scale_matrix.html` - India-specific payout, privacy, and integration assumptions

### Visual cues
- `deliverable_src/shared.css` - Existing collateral tone, colors, and document styling baseline

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `deliverable_src/shared.css`: Useful as a starting tone reference for color and editorial polish
- `deliverable_src/render_pdfs.sh`: Shows the current repo already supports investor-facing collateral generation

### Established Patterns
- There is no application code yet; this is a greenfield implementation effort.
- The current materials consistently favor offline-first trust, auditability, and India-first deployment constraints.

### Integration Points
- Future implementation should support both a field-agent mobile flow and an operator/investor dashboard.
- The Phase 1 shell should align with later parcel, evidence, payout, and audit phases instead of becoming a disconnected marketing prototype.

</code_context>

<deferred>
## Deferred Ideas

- AgriStack adapters and state-by-state integration depth
- Satellite-assisted anomaly review
- Full registry issuance automation
- Multi-country product expansion

</deferred>

---

*Phase: 01-premium-demo-foundation*
*Context gathered: 2026-03-26*
