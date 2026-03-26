---
phase: 1
slug: premium-demo-foundation
status: draft
shadcn_initialized: false
preset: joita-investor-premium
created: 2026-03-26
---

# Phase 1 - UI Design Contract

> Visual and interaction contract for the investor-ready Joita shell. Prepared from Phase 1 context and the existing investor materials.

---

## Visual Direction

Joita should look like premium trust infrastructure for climate-finance operations in India. The visual goal is calm authority: warm earth backgrounds, deep data surfaces, precise typography, and restrained highlight color. It should feel convincing in an investor meeting and practical enough to plausibly belong in the field.

Three adjectives:
- grounded
- precise
- premium

Three anti-goals:
- not generic green ESG slides
- not bureaucratic government software
- not flashy consumer-social UI

---

## Design System

| Property | Value |
|----------|-------|
| Tool | none |
| Preset | joita-investor-premium |
| Component library | custom primitives first |
| Icon library | Lucide |
| Font | Sora for display + IBM Plex Sans for body |

---

## Spacing Scale

Declared values (must be multiples of 4):

| Token | Value | Usage |
|-------|-------|-------|
| xs | 4px | Icon gaps, inline padding |
| sm | 8px | Compact element spacing |
| md | 16px | Default element spacing |
| lg | 24px | Section padding |
| xl | 32px | Layout gaps |
| 2xl | 48px | Major section breaks |
| 3xl | 64px | Page-level spacing |

Exceptions: none

---

## Typography

| Role | Size | Weight | Line Height |
|------|------|--------|-------------|
| Body | 15px | 450 | 1.6 |
| Label | 12px | 600 | 1.3 |
| Heading | 28px | 650 | 1.15 |
| Display | 44px | 700 | 1.05 |

Rules:
- Numeric KPI values may use a tighter tracking setting than body copy.
- Labels should stay short, declarative, and all-caps only for compact metadata.
- Avoid oversized marketing headlines that make the dashboard feel like a deck instead of a product.

---

## Color

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | #F6F1E7 | Background, light surfaces, map side panels |
| Secondary (30%) | #17352E | Navigation, dark cards, status rails, contrast surfaces |
| Accent (10%) | #C86B33 | Primary highlights, key proof states, focused map markers |
| Destructive | #B6422E | Rejection states and destructive confirmations only |

Accent reserved for: primary CTA, verified status emphasis, active map geometry, and one key metric per screen

Do not use accent for:
- all links
- every interactive control
- background floods
- decorative gradients with no meaning

---

## Layout Contracts

| Surface | Contract | Notes |
|---------|----------|-------|
| Agent mobile flow | One primary task per screen with persistent progress/status framing | Keep actions large and obvious for field use |
| Operator dashboard | Left-to-right story: summary -> map/proof -> timeline/actions | Make the proof chain easy to scan |
| Investor view | Hero KPI band plus one dominant proof visualization and one audit drill-down | Tell a story, do not create a trading terminal |
| Map screens | Parcel geometry is the hero element, surrounding chrome stays quiet | Avoid heavy map controls and clutter |

---

## Component Contracts

- Status chips use only four semantic families: pending, verified, flagged, paid.
- KPI cards must show plain-language labels before abbreviations or jargon.
- Evidence cards always show location, capture time, integrity state, and parcel linkage.
- Timeline rows must read as cause-and-effect events, not generic activity feed noise.
- Empty states should teach the next action instead of just announcing absence.

---

## Motion Contract

- Standard transition duration: 200ms ease-out
- Modal or drawer entrance: 220ms ease-out with subtle upward motion
- KPI card stagger: maximum 60ms between cards
- Map focus transition: smooth zoom/pan with no bounce effect
- Skeleton loading: soft shimmer on data surfaces only

Never use:
- springy overshoot on trust-critical surfaces
- looping decorative animations
- parallax or gimmick motion in dashboard views

---

## Copywriting Contract

| Element | Copy |
|---------|------|
| Primary CTA | Start field visit |
| Empty state heading | No verified activity yet |
| Empty state body | Capture parcel and evidence to unlock review, payout, and investor reporting. |
| Error state | We could not verify this event. Re-capture on-site or review the timeline details. |
| Destructive confirmation | Reject event: This removes the event from payout eligibility until corrected. |

Copy rules:
- Use calm, operational language.
- Prefer proof words over hype words: verified, captured, reviewed, paid.
- Avoid vague climate buzzwords unless the screen is explicitly investor-facing.

---

## Demo Storyboard Contract

1. Start with one named farmer and one clear parcel summary.
2. Show parcel mapping as a confident field action, not as a technical GIS exercise.
3. Transition into live evidence capture with visible proof tokens.
4. Move into an operator review timeline that explains the approval decision.
5. Show payout readiness and payout event as the farmer outcome.
6. End on an investor drill-down that ties map, evidence, payout, and audit history together.

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| shadcn official | none | not required |
| third-party | none | not applicable |

---

## Checker Sign-Off

- [ ] Dimension 1 Copywriting: PASS
- [ ] Dimension 2 Visuals: PASS
- [ ] Dimension 3 Color: PASS
- [ ] Dimension 4 Typography: PASS
- [ ] Dimension 5 Spacing: PASS
- [ ] Dimension 6 Registry Safety: PASS

**Approval:** pending manual review
