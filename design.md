# Design System

> Single visual language shared by the **web** client (`web/src/styles.css`) and
> the **Android** client (`android/.../ui/theme/`). The tokens below are the real
> values in those files, not aspirational ones.
>
> Change tokens **in both places in the same commit**, or the platforms drift.

---

## 1. Principles

1. **Dark by default.** Video is the product; the chrome recedes.
2. **Content-forward.** Poster art and playback dominate; UI is quiet scaffolding.
3. **One accent.** Red means *brand / primary action / active state*. Nothing else.
4. **Surfaces, not shadows.** Depth comes from stacked panel colors; shadows only for overlays.
5. **Legible dim ≠ invisible.** `text-dim` is for metadata, never for a primary action.

---

## 2. Color

### Core tokens

| Token | Web (CSS var) | Android | Value | Role |
|-------|---------------|---------|-------|------|
| Background | `--bg` | `Bg` | `#0B0D12` | Page / screen background |
| Panel | `--panel` | `Panel` | `#141821` | Cards, inputs, banners |
| Panel 2 | `--panel-2` | `Panel2` | `#1B2130` | Raised / hovered surfaces, menus |
| Text | `--text` | `Text` | `#E7ECF3` | Primary text |
| Text dim | `--text-dim` | `TextDim` | `#8B94A7` | Metadata, hints, placeholders |
| Accent | `--accent` | `Accent` | `#E50914` | Brand, primary button, active state |
| Accent 2 | `--accent-2` | `Accent2` | `#3B82F6` | Focus rings only |
| On accent | *(implicit `#fff`)* | `OnAccent` | `#FFFFFF` | Text/icons on `accent` |
| Divider | *(inline `#262D3D`)* | `Divider` | `#262D3D` | Input borders, separators |
| Scrim | *(inline `rgba(0,0,0,.6)`)* | `Scrim` | `#99000000` | Modal / overlay backdrop |

### Semantic colors

| Meaning | Web | Android | Notes |
|---------|-----|---------|-------|
| Success | `#4ADE80` | *(not yet defined)* | Settings probe "connected" |
| Error text | `#FF8A8A` | `ErrorRed #EF4444` | ⚠️ **divergence — see §8** |
| Error surface | `#2A1215` | *(not yet defined)* | Banners, playback errors |
| Video letterbox | `#000000` | `Color.Black` | Behind the `<video>` / `Surface` |

> ⚠️ **Android `ErrorRed` (`#EF4444`, Material red-500) does not match the web
> `#FF8A8A`.** It was added to resolve an unresolved-symbol defect. If you touch
> error styling, reconcile them — web's softer red reads better on `#0B0D12`.

### Border scale (web, not yet tokenized)

| Purpose | Value | Used by |
|---------|-------|---------|
| Input / icon button | `#262D3D` | `.search input`, `.icon-btn`, `.season-btn` |
| Elevated surface | `#2B3446` | `.modal`, `.quality-list`, `.btn-ghost` |
| Hover | `#232B3B` | `.btn-ghost:hover`, `.quality-item:hover` |

These are hardcoded hexes in CSS while Android already uses `Divider` for the
first one — a candidate for promoting to `--border`, `--border-raised`, `--hover`.

---

## 3. Typography

**Family:** `system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`
**Mono:** `ui-monospace, monospace` (Settings input only — it holds a URL)

| Role | Size | Weight | Line-height | Example |
|------|------|--------|-------------|---------|
| Detail heading | 26 px | — | — | `.detail-info h1` |
| Brand | 22 px | 800 | — | `.brand`, letter-spacing `-0.5px` |
| Modal heading | 18 px | — | — | `.modal h2` |
| Body | 14 px | — | 1.6 | `.detail-desc`, inputs |
| Controls | 13 px | 600 (primary) | — | buttons, list rows |
| Hint | 12.5 px | — | 1.5 | `.modal-hint` |
| Label | 12 px | — | — | `.modal-label`, `.player-sub` |
| Micro | 11 px | — | — | `.card-sub`, `.quality-note` |

**Clamping:** poster titles clamp to **2 lines** via `-webkit-line-clamp: 2`.
Never let a title push a grid row out of alignment.

---

## 4. Spacing & radius

| Scale | Values |
|-------|--------|
| Container | `max-width: 1200px`, `padding: 0 20px 60px` |
| Grid | `minmax(150px, 1fr)`, `gap: 16px` |
| Gaps | 4 / 6 / 8 / 10 / 14 / 16 / 20 / 24 px |
| Radius | `10px` surfaces · `14px` modal · `8px` controls · `7px` menu rows |

Radius is deliberately **not** uniform: bigger surfaces get bigger radii so nested
elements don't look "double-rounded".

---

## 5. Surfaces & elevation

| Level | Token | Used for |
|-------|-------|----------|
| 0 | `bg` | page |
| 1 | `panel` | cards, inputs, banners |
| 2 | `panel-2` | menus, hovered controls |
| Overlay | `panel` + shadow | modal: `0 24px 60px rgba(0,0,0,.6)`; menu: `0 12px 32px rgba(0,0,0,.5)` |

Elevation = **a lighter surface first**, shadow second. Never shadow alone on a
flat dark background — it's invisible.

---

## 6. Components

| Component | Structure | Active state |
|-----------|-----------|--------------|
| **Primary button** | `accent` bg, white text, radius 8, `font-weight: 600` | — |
| **Ghost button** | `panel-2` bg, `1px #2B3446`, radius 8 | hover → `#232B3B`; `disabled → opacity .5` |
| **Icon button** | 40×40, `panel` bg, `1px #262D3D` | hover → `panel-2` |
| **Input** | `panel` bg, `1px #262D3D`, radius 10, padding `10px 14px` | focus → `2px accent-2` outline, border transparent |
| **Poster card** | image `aspect-ratio: 2/3`, radius 10, metadata below | hover → `translateY(-4px)` over `0.15s ease` |
| **Season/episode chip** | `panel` bg, `1px #262D3D`, radius 8 | `.is-active` → `accent` bg, white, 600 |
| **Modal** | `panel`, `1px #2B3446`, radius 14, max-width 460, scrim behind | — |
| **Quality menu** | anchored `panel-2`, radius 10, min-width 170 | row `.is-active` → `accent` bg, white |
| **Error banner** | `#2A1215` bg, `#FF8A8A` text, radius 10 | — |

**Focus** is always `2px solid accent-2` — never removed. A hidden focus ring on
a keyboard-driven UI is a defect.

---

## 7. Motion

| Interaction | Value | Where |
|-------------|-------|-------|
| Card hover lift | `transform 0.15s ease`, `translateY(-4px)` | `.card` |
| Control hover | background swap, no duration set | buttons, rows |

**Rules:** motion confirms state, never gates content. No infinite animation
besides playback itself. If `prefers-reduced-motion` is added, the card lift is
the only thing that should be disabled.

---

## 8. Platform parity

Android mirrors these tokens **as hex constants in `Color.kt`**, not from XML
resources, so the theme compiles without a `Resources` lookup and stays
line-by-line comparable to the CSS. The header comment in `Color.kt` points back
at `styles.css`.

**Known divergences to reconcile:**

- [ ] Error red: web `#FF8A8A` vs Android `#EF4444`
- [ ] Success green: web `#4ADE80`, no Android equivalent
- [ ] Border scale: hardcoded in CSS, tokenized in Android

**Rule:** a token added to one platform must be added to the other in the same
commit, or recorded here as an explicit gap.

---

## 9. Responsive (web)

Breakpoint `max-width: 640px`:
- `.detail-head` → column, cover shrinks 180 px → 130 px
- `.topbar` → column, full-width, gap 10 px

Grid is already fluid via `auto-fill minmax(150px, 1fr)` — no breakpoint needed.

**Android TV (planned)** scales differently: 1.5× type and spacing, D-pad focus
scales the focused card. That is a separate layout, not a media query.

---

## 10. Accessibility checklist

- [x] Text on `accent`: white on `#E50914` (sufficient contrast)
- [x] Focus visible everywhere (`accent-2` ring)
- [x] Touch targets ≥ 40 px (`.icon-btn`)
- [ ] `prefers-reduced-motion` honored
- [ ] Verified contrast for `text-dim` `#8B94A7` on `bg` `#0B0D12` (≈ 6.1:1 — passes AA for body text)
- [ ] Keyboard traversal order audit across modal → menu
