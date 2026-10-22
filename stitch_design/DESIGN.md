---
name: Mindful Sanctuary
colors:
  surface: '#130c35'
  surface-dim: '#130c35'
  surface-bright: '#39345e'
  surface-container-lowest: '#0d0630'
  surface-container-low: '#1b153e'
  surface-container: '#1f1942'
  surface-container-high: '#2a244d'
  surface-container-highest: '#352f59'
  on-surface: '#e5deff'
  on-surface-variant: '#b9cacb'
  inverse-surface: '#e5deff'
  inverse-on-surface: '#302b54'
  outline: '#849495'
  outline-variant: '#3b494b'
  surface-tint: '#00dbe9'
  primary: '#dbfcff'
  on-primary: '#00363a'
  primary-container: '#00f0ff'
  on-primary-container: '#006970'
  inverse-primary: '#006970'
  secondary: '#adc6ff'
  on-secondary: '#002e6a'
  secondary-container: '#0566d9'
  on-secondary-container: '#e6ecff'
  tertiary: '#fff3f5'
  on-tertiary: '#640039'
  tertiary-container: '#ffccdd'
  on-tertiary-container: '#b4146e'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#7df4ff'
  primary-fixed-dim: '#00dbe9'
  on-primary-fixed: '#002022'
  on-primary-fixed-variant: '#004f54'
  secondary-fixed: '#d8e2ff'
  secondary-fixed-dim: '#adc6ff'
  on-secondary-fixed: '#001a42'
  on-secondary-fixed-variant: '#004395'
  tertiary-fixed: '#ffd9e4'
  tertiary-fixed-dim: '#ffb0cd'
  on-tertiary-fixed: '#3e0022'
  on-tertiary-fixed-variant: '#8c0053'
  background: '#130c35'
  on-background: '#e5deff'
  surface-variant: '#352f59'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.02em
  display-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  title-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0em
  title-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
    letterSpacing: 0.01em
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.01em
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.01em
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 18px
    letterSpacing: 0.02em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.04em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.06em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

The design system establishes a reflective, frictionless, and emotionally stabilizing environment tailored for anti-impulse shopping intervention. Built specifically for an intentional pause experience, the design counteracts predatory e-commerce urgency patterns with deep, serene space and slow-reveal micro-interactions.

The aesthetic blends **Modern Material 3 glassmorphism** with an atmospheric, cosmic deep-indigo ambient space. Instead of sensory deprivation, the interface employs an iridescent refraction aesthetic: rich, dark indigo surfaces accented by crystalline frosted glass cards, subtle luminous borders, and an ethereal spectrum gradient (cyan to amber) used exclusively to celebrate restraint, friction completion, and mindful decisions.

Key experiential tenets:
- **Calm Over Friction:** Cooling-off mechanisms must feel rewarding and meditative, not punitive.
- **Atmospheric Depth:** Visual layers step toward the user using tonal saturation, subtle indigo back-glows, and frosted translucent scrims.
- **Clarity of Status:** State signifiers (Locked, Ready, Discarded, Proceeded) receive distinct, luminous tonal treatments to eliminate cognitive load.

## Colors

The palette is tuned specifically for high-contrast legibility in a 100% dark mode context, engineered against dark indigo foundations.

### Surface Tones & Canvas
- **Canvas Base:** `#090422` (deepest void background) to `#0D0630` (root canvas).
- **Surface Level 1 (Base Container):** `#160E3B`
- **Surface Level 2 (Interactive Container / Cards):** `#1E154A`
- **Surface Level 3 (Elevated / Dialogs):** `#281D60`
- **Surface Level 4 (Active Floating States / Modals):** `#322575`

### Text & Iconography
- **Text Primary:** `#F2F0FA` — crisp off-white with slight cool violet undertone for reduced eye strain.
- **Text Secondary / Muted:** `#A9A3C9` — mid-tone lavender for descriptions, secondary metadata, and inactive icons.
- **Text Tertiary / Disabled:** `#7F77A8` — deep muted lilac for micro-labels, placeholders, and subtle borders.

### Signature Iridescent Gradient
Reserved for progress rings, successful unlock rings, celebration states, and the primary intentional action trigger:
- `linear-gradient(135deg, #00F0FF 0%, #3B82F6 34%, #EC4899 68%, #F59E0B 100%)`

### Semantic Status Tokens
- **LOCKED / Cooling (Active Delay):** `#FBBF24` (Primary), `#F59E0B` (Deep shade), Container: `rgba(245, 158, 11, 0.12)`
- **UNLOCKED / Ready (Deliberate Choice):** `#34D399` (Primary), `#10B981` (Deep shade), Container: `rgba(16, 185, 129, 0.12)`
- **DISCARDED / Saved (Impulse Resisted):** `#94A3B8` (Primary), `#64748B` (Deep shade), Container: `rgba(148, 163, 184, 0.10)`
- **PURCHASED / Proceeded (Mindful Checkout):** `#38BDF8` (Primary), `#60A5FA` (Deep shade), Container: `rgba(56, 189, 248, 0.12)`

## Typography

The design system employs **Plus Jakarta Sans** across all hierarchies. Its clean geometric architecture, generous x-height, and contemporary humanist curves provide immaculate legibility on OLED screens and convey an inviting, modern sensibility.

- Numeric time displays for countdown timers must utilize tabular/proportional figures (`font-feature-settings: "tnum" on`) to eliminate jitter during active cooling-down increments.
- Status badges and micro-chips utilize `label-sm` or `label-md` with uppercase transformation and slight letter spacing (`0.04em`–`0.06em`) to ensure instant legibility against blurred surfaces.

## Layout & Spacing

Engineered natively for the standard Android mobile viewport (412x915 dp), the system adopts a strict 8dp vertical grid with 4dp sub-increments for precise icon-to-text optical alignment.

- **Canvas Padding:** Standard horizontal margins are set to 16dp (`margin: 1rem`), expanding to 24dp for meditative empty states and flow-stopping modal interventions.
- **Card Gaps:** Vertical lists of cooling items utilize 16dp (`space-md`) spacing to preserve separation between competing temptations.
- **Bottom Navigation Clearance:** All scroll views include 104dp of bottom padding to clear the 72dp bottom navigation bar and the native Android system gesture inset.
- **Safe Areas:** System bars (status bar ~40dp, navigation bar ~24dp) are protected with dynamic inset handling. Top app bars occupy 64dp of dedicated height.

## Elevation & Depth

Visual hierarchy does not rely on harsh drop shadows; instead, it uses a fusion of **frosted glassmorphism**, **tonal surface climbing**, and **subtle indigo ambient radiance**.

### Layering Model
1. **Background Canvas (Elevation 0):** Pure gradient wash from `#090422` to `#0D0630`.
2. **Resting Cards (Elevation 1):** Translucent fill `rgba(30, 21, 74, 0.70)` overlaid with `backdrop-filter: blur(20px)`, framed by a delicate 1px border of `rgba(255, 255, 255, 0.08)`.
3. **Active / Highlighted Cards (Elevation 2):** Fill `rgba(40, 29, 96, 0.85)` with `backdrop-filter: blur(24px)`, 1px border `rgba(255, 255, 255, 0.15)`, and an ambient back-glow: `box-shadow: 0 12px 32px -4px rgba(13, 6, 48, 0.60), 0 0 24px -2px rgba(99, 102, 241, 0.15)`.
4. **Bottom Sheets & Floating Navigation (Elevation 3):** Fill `rgba(22, 14, 59, 0.90)` with `backdrop-filter: blur(28px)`, upper border `rgba(255, 255, 255, 0.12)`, shadow `0 -8px 32px rgba(9, 4, 34, 0.80)`.
5. **Countdown Timer Domes / Key Interventions (Elevation 4):** Radial back-glows tinted to the status (e.g., locked timer has a soft Amber `#F59E0B` glow with 20% opacity and 40px blur radius).

## Shapes

The design system embraces a high-radius, pill-forward shape taxonomy (`roundedness: 3`) that removes physical edges, inducing visual ease and subconscious stress reduction.

- **Main Cards & Intervention Panels:** 20dp to 24dp (`rounded-xl` / 1.5rem to 2rem) rounded corners.
- **Pills, Badges & Micro-Chips:** Full pill curvature (`border-radius: 9999px`).
- **Interactive Buttons:** Full pill geometry (`border-radius: 9999px`) for primary actions; 16dp rounded corners for secondary inline controls.
- **Progress Indicators:** Concentric circles and continuous curved arcs without sharp terminal caps.

## Components

### Buttons
- **Primary / Iridescent Action:** Pill container (`height: 52dp`, `padding: 0 24dp`). Gradient background (`#00F0FF` -> `#3B82F6` -> `#EC4899`). Label `label-lg` in `#090422` bold for high-contrast legibility.
- **Secondary / Mindful Friction Action:** Translucent surface `rgba(255, 255, 255, 0.08)`, 1px border `rgba(255, 255, 255, 0.14)`, text `#F2F0FA`. On press, transitions to `rgba(255, 255, 255, 0.14)`.
- **Destructive / Resisted Button:** Subtle ghost treatment, text `#94A3B8`, changing to `#F87171` only on deliberate tap-and-hold.

### Cards (Item Cooling Container)
- Sized for single-column feed. Background `rgba(30, 21, 74, 0.70)`, 20dp corner radius, 1px border `#ffffff15`.
- Layout: 80dp thumbnail with 14dp radius on the left; central column with product title (`title-md`), retailer tag, and timestamp; right column hosting the status badge or circular countdown micro-indicator.

### Circular Countdown Timers
- Component diameter: 88dp (card view) or 196dp (detail view).
- Background track: `rgba(255, 255, 255, 0.08)` stroke width 6dp.
- Active progress stroke: Amber `#FBBF24` for locked duration, smoothly interpolating to Mint `#34D399` upon timer completion. Includes a subtle center readout in tabular `display-md` or `title-md`.

### Status Badges & Location Micro-Chips
- **Status Badges:** Height 28dp, pill shape. Subtle tonal background (12% opacity of semantic color) with 1px border (24% opacity) and corresponding icon + `label-md` text.
- **Location Micro-Chips:** For geofenced impulse physical triggers ("Near Best Buy"). Height 24dp, pill-shaped, background `rgba(40, 29, 96, 0.8)`, icon 14dp, text `body-sm` in `#A9A3C9`.

### Input Fields
- Height 56dp. Surface `#160E3B` with 16dp radius and 1px border `rgba(255, 255, 255, 0.10)`.
- Floating label in `#A9A3C9`, transitioning to `#00F0FF` on focus. Focused border uses 1.5px `#00F0FF` with a 4px soft outer cyan halo.

### Checkboxes & Toggle Controls
- 24dp rounded squares (8dp radius) for selection controls.
- Unchecked: 1.5px border `#7F77A8`.
- Checked: Iridescent fill or Emerald fill with `#090422` bold checkmark glyph.

### Bottom Navigation Bar
- Android standard 4-tab bar (Height 72dp) floating with 16dp margins or anchored to bottom edge.
- Surface `rgba(22, 14, 59, 0.92)` with `backdrop-filter: blur(24px)` and top border `rgba(255, 255, 255, 0.08)`.
- Tabs: **Cooling Down** (Hourglass/Sparkle), **Ready** (Checkmark-Circle), **Impulse Map** (Radar/Pin), **Saved** (Vault/Piggy-Bank).
- Active tab displays an iridescent indicator pill behind the icon with glowing `#F2F0FA` icon and text label; inactive tabs use `#7F77A8`.