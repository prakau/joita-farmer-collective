---
phase: 7
slug: rebuild-soildetector-as-joita-bioseed-ai-bilingual-android-app
status: approved
shadcn_initialized: false
preset: none
created: 2026-08-11
---

# Phase 7 — UI Design Contract

> Android visual and interaction contract for **JOITA BioSeed AI – Soil Saathi**. User decisions come from `07-CONTEXT.md`; Android implementation details are mandatory unless explicitly marked discretionary.

## Design System

| Property | Value |
|----------|-------|
| Tool | Native Android, Jetpack Compose Material 3 |
| Preset | Not applicable |
| Component library | Material 3 primitives with JOITA wrappers |
| Icons | Material Symbols Rounded; no icon without a text/content description |
| Font | System sans with Devanagari fallback (`sans-serif`); no downloadable font dependency |
| Brand asset | Exact supplied full JOITA BIOSEED AI PNG for splash/sign-in/About; never stretch, recolor, or crop the wordmark |
| App name | JOITA BioSeed AI – Soil Saathi |
| Hindi name | JOITA बायोसीड AI – मिट्टी साथी |

## Spacing and Shape Scale

| Token | Value | Usage |
|-------|-------|-------|
| `space-1` | 4dp | Status dot, micro-gap |
| `space-2` | 8dp | Icon-to-label, compact rows |
| `space-4` | 16dp | Default screen/card padding |
| `space-6` | 24dp | Section separation |
| `space-8` | 32dp | Hero separation |
| `space-12` | 48dp | Empty/success state breathing room |
| `space-16` | 64dp | Major splash spacing only |

Exceptions: interactive targets have a minimum 48×48dp hit area; bottom navigation is 72dp high (required to hold a 24dp icon, two-line bilingual-safe label, and safe vertical padding); compact status chips may render 32dp high only when their entire row remains tappable at 48dp. Chip internals use 8dp and input internals use 16dp from the declared scale.

| Shape | Value | Usage |
|-------|-------|-------|
| Small | 8dp | Chips, data badges |
| Medium | 12dp | Inputs and secondary buttons |
| Large | 20dp | Cards, sheets, primary buttons |
| Hero | 28dp | Home action panel and result score |

Elevation is limited to 0dp surfaces, 1dp cards, 3dp sticky action bars, and 6dp modal sheets.

## Typography

Use exactly two weights: regular 400 and semibold 600. Respect Android font scaling through 200%; never force `fontScale = 1`.

| Role | Size | Weight | Line Height |
|------|------|--------|-------------|
| Label/body-small | 14sp | 400/600 | 20sp |
| Body/button | 16sp | 400/600 | 24sp |
| Section heading | 20sp | 600 | 28sp |
| Screen/display | 28sp | 600 | 36sp |

Numeric sensor values may use 28sp/600 with tabular figures. Hindi and English must use the same sizes and hierarchy.

## Color

| Token | Value | Usage |
|-------|-------|-------|
| `joitaGreen` | `#238B2A` | Primary actions, selected navigation, live/healthy state |
| `joitaGreenDark` | `#125C2A` | Headers, on-light emphasis, launcher background |
| `leafLight` | `#DDF3DD` | Success/selected containers |
| `cream` | `#FFF9ED` | Dominant 60% app background |
| `surface` | `#FFFFFF` | Secondary 30% cards, sheets, inputs |
| `ink` | `#111817` | Primary text and on-light icons |
| `inkMuted` | `#59645E` | Secondary text; minimum 4.5:1 contrast |
| `line` | `#D9E2DA` | Dividers and input outlines |
| `turmeric` | `#F4A622` | Accent 10% for pending sync, important tips, score highlights |
| `soil` | `#7A4E2D` | Soil/sample context and chart series only |
| `error` | `#BA1A1A` | Destructive/error states only |
| `info` | `#1769AA` | GPS/network informational status |

Accent is reserved for pending/offline status, the home “next step” marker, important agronomy tips, and result-chart highlights. Primary buttons remain green. Never encode sensor health, connectivity, or risk by color alone; pair color with text and icon.

Dark theme is out of scope for this release. System status/navigation bars use `cream` or `joitaGreenDark` with contrast-correct icons.

## Navigation Contract

Use one activity and four top-level destinations in a 72dp bottom bar:

1. **Home / होम** — readiness, next action, recent result.
2. **Fields / खेत** — farmer and field profiles.
3. **Tests / जाँच** — sensor/manual/sample entry and history.
4. **More / अधिक** — sync, guidance, language, contact, privacy, About.

The primary flow is linear and resumable: `Home → Select farmer/field → Choose reading source → Capture reading → Add GPS/photo → Review → Soil result → Advice → Save/share report`.

- Back always returns to the previous step without deleting entered data.
- Exiting an incomplete test offers **Save test draft / जाँच का ड्राफ्ट सहेजें** or **Discard test / जाँच हटाएँ**.
- Bottom navigation is hidden inside the test wizard and report preview.
- A sticky bottom action bar keeps one primary action and at most one text secondary action.

## Screen Inventory

### 1. Splash and first-run language

- Full logo centered at max width 280dp with 24dp outer padding and `ContentScale.Fit`.
- Subtitle: “Soil decisions, made simple” / “मिट्टी की सही जानकारी, आसान तरीके से”.
- First run shows two equal language cards: **English** and **हिन्दी**. Selection persists and also integrates with Android per-app locale settings.
- Consent copy links to Privacy and Terms; no permissions are requested here.

### 2. Home readiness dashboard

- Header: logo mark/avatar, current language chip, overflow.
- Greeting uses field-agent/farmer-neutral copy: “Ready for today’s field work?” / “आज खेत की जाँच के लिए तैयार हैं?”
- Three explicit status chips: `Offline/Online`, `Location ready/Not captured`, `Sensor connected/Not connected`.
- Hero card primary CTA: **Start soil test / मिट्टी की जाँच शुरू करें**.
- Secondary cards: Resume draft, Add field, Recent soil health, Pending sync.
- Offline is a normal state: “Offline — your work is saved on this phone” / “ऑफ़लाइन — आपका काम इस फ़ोन में सुरक्षित है”.

### 3. Farmer and field profiles

- Searchable list with farmer name, village, field size, last test date, sync badge.
- Add/edit form fields: farmer name, mobile (optional), village, district, state, pincode, Khasra/plot number, area, unit acre/hectare, main crop.
- Required markers appear in label and validation text; no government identifier field.
- Empty: “No fields added yet” / “अभी कोई खेत नहीं जोड़ा गया” and CTA **Add first field / पहला खेत जोड़ें**.

### 4. Reading-source chooser

Three vertically stacked cards, each with title, one-line explanation, and status:

- **Connect USB sensor / USB सेंसर जोड़ें** — live probe reading; shows OTG availability and attached-device state.
- **Enter manually / हाथ से दर्ज करें** — laboratory or another meter; requires source note.
- **Try sample reading / नमूना रीडिंग देखें** — clearly labeled demo; never saved without a `Sample` badge.

USB states: unsupported, no device, permission needed, connecting, connected, reading, unstable/partial payload, disconnected, retry. Permission rationale must explain OTG access before opening the system permission dialog.

### 5. Live sensor and manual entry

- Live panel shows eight tiles: Moisture %, Temperature °C, EC µS/cm, pH, Nitrogen mg/kg, Phosphorus mg/kg, Potassium mg/kg, Fertility mg/kg.
- Sensor values remain `—` until a complete payload arrives. Use `Live sensor` badge only for parsed device data.
- Sensor CTA states: **Connect sensor**, **Allow USB access**, **Take reading**, **Try again**.
- Manual screen uses numeric keyboards, field-level ranges and unit suffixes; source options Laboratory/Other meter/Estimated plus required note for Estimated.
- A 10-second stable-reading indicator can be used, but users can capture after one complete valid payload.

### 6. Evidence and location

- GPS card displays village/coordinates, accuracy in metres, timestamp, and **Capture location / स्थान दर्ज करें**.
- Permission denied state offers **Open settings / सेटिंग खोलें** and **Enter location manually / स्थान हाथ से भरें**.
- Photo card launches camera only after rationale; gallery import is excluded from the trusted evidence path.
- Camera/location are optional for saving a basic reading but report trust status must show what is missing.

### 7. Review, result, and advice

- Review separates `Observed`, `Entered context`, and `Missing evidence` sections before **Save soil test / मिट्टी जाँच सहेजें**.
- Result hero shows 0–100 **Soil health score / मिट्टी स्वास्थ्य स्कोर** plus plain status Good / Needs attention / Urgent review. Score must show “advisory” label.
- Parameter rows show observed value, reference band, and status icon/text.
- Advice cards group “What this means”, “Next field action”, and “Discuss with an expert”.
- Recommendations use Indian units (`kg/acre`) and state they depend on crop/region; never claim guaranteed yield or certified diagnosis.
- Disclaimer: “Advisory only. Confirm major fertilizer or treatment decisions with a soil laboratory or qualified agronomist.” / “यह केवल सलाह है। खाद या उपचार के बड़े निर्णय से पहले मिट्टी प्रयोगशाला या योग्य कृषि विशेषज्ञ से पुष्टि करें।”

### 8. History and report

- Filter by farmer, field, date and source (Sensor/Manual/Sample).
- Report preview includes JOITA logo, farmer/field, timestamp, GPS/evidence trust status, measurements, advisory score, actions, disclaimer and unique local report ID.
- Actions: **Share report / रिपोर्ट साझा करें**, **Save PDF / PDF सहेजें**, **Call support / सहायता के लिए कॉल करें**, and **WhatsApp support / WhatsApp सहायता** through explicit Android intents.
- If WhatsApp or a PDF viewer is unavailable, show a clear alternative share sheet or copy-support-number action.

### 9. Sync, guidance, settings, About

- Sync screen groups Ready, Syncing, Failed with retry, and Synced. Without configured backend, show `Local only — cloud sync not configured`; never fake success.
- Guidance includes probe preparation, OTG help for common Indian phone brands, pH/NPK/EC explanations, sampling tips, and safety/disclaimer content in both languages.
- Settings: language, area unit, text size (system/default/large), data export, clear demo data, privacy/terms.
- About shows exact full logo and “Made by JOITA BIOSEED AI / JOITA BIOSEED AI द्वारा निर्मित”, version, package, and support contact placeholders clearly marked for configuration.

## Component and State Contract

- `JoitaTopBar`: title ≤2 lines; optional back; language/status actions have content descriptions.
- `ReadinessChip`: icon + short label + semantic container; never icon-only.
- `PrimaryActionCard`: one verb-led CTA and a single supporting sentence.
- `SensorMetricTile`: label, value, unit, source/status; skeleton/pulse only while actively reading.
- `FieldTextInput`: persistent label, inline validation, unit suffix; never placeholder-only.
- `StepHeader`: “Step X of 4 / चरण X/4”, title, optional **Save test draft / जाँच का ड्राफ्ट सहेजें**.
- `OfflineBanner`: persistent but non-blocking, dismissible only for current session.
- `PermissionSheet`: benefit, data handling, permission-specific action such as **Continue to USB access / USB अनुमति दें**, **Continue to camera / कैमरा अनुमति दें**, or **Continue to location / स्थान अनुमति दें**, plus **Not now / अभी नहीं**; the system dialog follows the specific continue action.
- `SourceBadge`: Live sensor (green), Manual (blue), Sample (turmeric), always text-visible.
- `DestructiveDialog`: names the farmer/field/test and explains local-only deletion; actions **Keep test / जाँच रखें** and **Delete test / जाँच हटाएँ** (or the corresponding Farmer/Field noun).

Loading, empty, offline, permission-denied, unsupported-hardware, validation-error, partial-data, success, and retry states are required wherever applicable. Snackbar actions remain visible at least 6 seconds.

## Copywriting Contract

| Element | English | Hindi |
|---------|---------|-------|
| Primary CTA | Start soil test | मिट्टी की जाँच शुरू करें |
| Empty heading | No soil tests yet | अभी कोई मिट्टी जाँच नहीं है |
| Empty body | Start a test with a USB sensor or enter a reading manually. | USB सेंसर से जाँच शुरू करें या रीडिंग हाथ से दर्ज करें। |
| USB error | Sensor not found. Check the OTG cable, enable OTG in phone settings, then try again. | सेंसर नहीं मिला। OTG केबल जाँचें, फ़ोन सेटिंग में OTG चालू करें, फिर दोबारा कोशिश करें। |
| Offline | Offline — saved safely on this phone | ऑफ़लाइन — इस फ़ोन में सुरक्षित |
| Delete test | Delete this test? This removes it from this phone and cannot be undone. | यह जाँच हटाएँ? यह इस फ़ोन से हट जाएगी और वापस नहीं लाई जा सकेगी। |
| Sync unavailable | Local only — cloud sync is not configured | केवल इस फ़ोन में — क्लाउड सिंक सेट नहीं है |

Use sentence case, active verbs, and plain language. Avoid “Submit”, “Proceed”, “Invalid”, and unexplained English acronyms in Hindi; define N-P-K, EC, GPS, USB and OTG on first use.

## Accessibility and Responsive Rules

- Minimum 48dp targets and 8dp separation between adjacent targets.
- Body/text contrast ≥4.5:1; large text and icons ≥3:1.
- TalkBack traversal follows visual order; every metric announces label, value, unit, source and status.
- Do not lock portrait, font scale, or display size. Support 320dp width, 200% font scale, keyboard navigation, and landscape without clipped primary actions.
- At width <360dp, metric grid becomes one column; ≥360dp uses two columns; ≥600dp uses two-pane list/detail where useful.
- Hindi strings may be 35% longer; buttons wrap to two lines and grow vertically rather than truncate.
- Animations respect reduced-motion settings and stay under 300ms; no essential information is motion-only.
- Forms preserve state across configuration change and process recreation.

## Registry Safety

No shadcn or third-party UI registry is used. Only declared Android/AndroidX dependencies may supply UI code. The USB serial dependency is functional infrastructure and must be pinned to a reviewed release.

## Visual Acceptance Criteria

- Full logo renders with original aspect ratio on Splash and About at 320dp and 600dp widths.
- Home exposes network, GPS and sensor states plus `Start soil test` without scrolling at 360×800dp under 100% font scale.
- Every screen remains operable at 320dp width and 200% font scale with no clipped primary CTA.
- English and Hindi locale screenshots cover Home, source chooser, live sensor, result, report and empty/error states.
- USB unsupported, permission denied, disconnected and partial-payload states each show a recovery action.
- Sensor/manual/sample readings have permanent source badges in review, history and reports.
- Offline mode can create a field, save a manual test, view history and open a report without network.
- Accessibility scanner finds no touch-target or unlabeled-control errors in top-level flows.

## Checker Sign-Off

- [x] Dimension 1 Copywriting: PASS
- [x] Dimension 2 Visuals: PASS
- [x] Dimension 3 Color: PASS
- [x] Dimension 4 Typography: PASS
- [x] Dimension 5 Spacing: PASS
- [x] Dimension 6 Registry Safety: PASS

**Approval:** approved 2026-08-11
