# Phase 7: JOITA BioSeed AI – Soil Saathi - Context

**Gathered:** 2026-08-11
**Status:** Ready for UI design and planning
**Source:** User-provided APK, logo, and product direction

<domain>
## Phase Boundary

Create a new, maintainable Android application inspired by the supplied SoilDetector APK, but with a completely new India-first interface and product identity. Deliver an installable APK named **JOITA BioSeed AI – Soil Saathi** with English and Hindi only, using the supplied JOITA BIOSEED AI logo. The app must work offline for core field workflows and use Android phone capabilities where permission is granted.

</domain>

<decisions>
## Implementation Decisions

### Brand and naming
- App display name: **JOITA BioSeed AI – Soil Saathi**.
- Hindi product descriptor: **JOITA बायोसीड AI – मिट्टी साथी**.
- Android package namespace: `ai.joita.biosoil`.
- Publisher/About attribution: **Made by JOITA BIOSEED AI** / **JOITA BIOSEED AI द्वारा निर्मित**.
- Use the exact supplied JOITA BIOSEED AI logo without redrawing or changing the wordmark.

### Language
- Ship only English (`en`) and Hindi (`hi`) product copy.
- Provide an in-app language switch that updates the interface immediately and persists locally.
- Use short, field-friendly bilingual labels and plain Hindi rather than technical literal translations.

### India-first field experience
- Large touch targets, clear numbered steps, high contrast, and low-literacy-friendly icons.
- Core workflows: farmer/farm profile, GPS-tagged field, soil test entry, camera evidence, soil health result, crop/fertilizer recommendations, history, offline queue, and report sharing.
- Indian units and terminology: acre/hectare, kg/acre, quintal, village, district, state, pincode, Khasra/plot number, pH, N-P-K and organic carbon.
- Include a transparent disclaimer that recommendations are advisory and laboratory/agronomist confirmation is recommended.

### Mobile integration
- Preserve the reference app's USB-OTG soil probe workflow as an optional hardware path: detect compatible USB serial devices, request Android USB permission explicitly, communicate at 9600 baud / 8 data bits / 1 stop bit / no parity, and expose connection/error/retry state in plain language.
- Support the reference device IDs for vendor `6790` and product IDs `29987`, `21795`, and `21778`; also allow the serial library's safe default probing for compatible adapters.
- Parse the reference probe payload into moisture, temperature, conductivity/EC, pH, nitrogen, phosphorus, potassium, and fertility values. Keep the original reference APK untouched and document the protocol basis in the new source.
- USB host capability must be declared optional so non-OTG phones can install the app and use manual entry, saved history, guidance, and reports.
- Camera capture for field/soil evidence using Android runtime permission handling.
- GPS capture with visible permission, accuracy, timestamp, and manual fallback.
- Phone and WhatsApp hand-offs use Android intents; no hidden calls or messages.
- Local-first storage for profiles, observations, reports, language, and pending sync state.
- Visible offline/online state; the app must remain useful without network access.

### Compatibility and distribution
- Build a signed installable Android APK for arm64-v8a, armeabi-v7a, x86, and x86_64 where the chosen toolchain supports universal output.
- Minimum Android version should favor broad active-device compatibility without using obsolete storage/security practices.
- Request only permissions needed by explicit user actions.
- Preserve the original APK unchanged and place the new app in a new top-level folder.

### Visual system
- Brand foundation: JOITA leaf green and near-black from the supplied logo.
- Supporting palette: deep leaf green, fresh green, turmeric/saffron accent, soil brown, warm cream surfaces, and clear success/warning/error colors.
- Use rounded cards, a prominent primary action, concise bottom navigation, and clear offline/GPS status.
- The interface must be completely new, not a recolored clone of the supplied APK.

### the agent's Discretion
- Exact Android framework, database library, component implementation, target SDK, testing strategy, and non-sensitive sample data.
- Exact screen composition and iconography within the locked brand and accessibility constraints.
- Whether remote sync is represented by a safe local queue/demo adapter when no production backend credentials are available.
- Defensive buffering/checksum validation around the recovered serial payload and how manual/demo readings are labeled when physical hardware is absent.

</decisions>

<canonical_refs>
## Canonical References

### Product direction
- `.planning/PROJECT.md` — JOITA offline-first, India-first, low-end Android, privacy, evidence, and cost constraints.
- `.planning/REQUIREMENTS.md` — existing farmer, mapping, evidence, verification, payout, and compliance requirements.
- `.planning/ROADMAP.md` — Phase 7 boundary and milestone ordering.

### User-supplied inputs
- `/Users/meenakshi/Downloads/SoilDetector_3.2.0_release_20260628_190347.apk` — reference APK; preserve unchanged.
- `/var/folders/vk/k2_cllgn58bdnmdg_q0786zm0000gn/T/codex-clipboard-a3020b0b-7ce3-40f8-9351-ed9b86567160.png` — approved JOITA BIOSEED AI logo.

</canonical_refs>

<specifics>
## Specific Ideas

- Home screen should answer three questions immediately: current connectivity, current location readiness, and the next field action.
- Soil result should present an understandable health score and separate observed values from advisory suggestions.
- Sensor screen should have three explicit sources: **Connect USB sensor**, **Enter manually**, and **Try sample reading**. Never present sample/manual values as live sensor output.
- Reports should be easy to show on-screen and share through installed Android apps.
- Demo data should use Indian names/places but be visibly sample data.

</specifics>

<deferred>
## Deferred Ideas

- Production cloud authentication and real server synchronization require backend credentials and operational policy decisions.
- Certified agronomic recommendation models, laboratory integrations, government registry integrations, and automated subsidy/credit eligibility are outside this first installable build.
- Google Play publishing is separate from producing a locally installable signed APK.

</deferred>

---

*Phase: 07-rebuild-soildetector-as-joita-bioseed-ai-bilingual-android-app*
*Context gathered: 2026-08-11 from user direction*
