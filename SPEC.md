# VN Engine — Kotlin Multiplatform Visual Novel Engine

## Vision
A modern visual novel engine written in Kotlin, rivaling Ren'Py with better type safety, performance, and tooling.

## Decisions

### Rendering
- **Compose for Desktop** — GPU-accelerated, declarative UI, good for 2D sprite layering

### DSL Design
- **Pure Kotlin DSL** — Users write actual Kotlin code with full IDE support, type safety, and compile-time checking. No custom parser needed.

### DSL API Style
- **Scope functions** — `scene { show(sprite, at = center) { dialogue("...") { choice(...) } } }` — nested builders

### Save/Load
- **Hybrid approach** — Snapshot key state via kotlinx.serialization + replay recent events. Compact saves without full replay cost.

### Save Migration
- **Forward-compatible format** — Design save format to be forward-compatible — extra fields ignored

### Asset Loading
- **Layered strategy**:
  - Persistent assets (UI, current characters) stay in memory
  - Scene-specific assets preload before scene starts
  - Other assets lazy-load on demand

### Hot Reload
- **Compose Hot Reload** — Use Compose's built-in hot reload for UI changes — free, no extra work

### Platforms
- **Desktop + Android** via Kotlin Multiplatform

### Effects & Transitions
- **Declarative in DSL** — `scene { fade(to = black, duration = 1.seconds) }` — type-safe, composable
- **Effect stack/pipeline** — Effects as reusable composable functions applied to scenes
- **Custom GPU shader slot** — Provide escape hatch for users to write their own GLSL shaders, but not default

### Complex Transitions
- **Declarative timeline** — Define a timeline of events with durations — engine plays them back (e.g., character walks while background changes)

### Audio
- **KMP audio library** — Multiplatform audio abstraction for consistent behavior across Desktop + Android (BGM, SFX, voice lines)

### Scene Graph
- **Tree/nested nodes** — Hierarchical scene graph for maximum flexibility. Game creators can structure scenes however they want, and AI tools can help generate and understand the structure.

### Localization / i18n
- **Type-safe generated** — Generate Kotlin classes from translation files — compile-time safety, IDE autocomplete

### Text Rendering
- **Custom text engine** — Per-character control, rich markup, font effects — more powerful. Sane defaults so users don't need to configure everything. Kotlin DSL makes the API ergonomic.

### Text Markup
- **Kotlin DSL builders** — `text { bold("..."); colored("...", red) }` — type-safe, IDE autocomplete, chainable (e.g., bold + different color). Also support inline background art (e.g., "BOOM!" with styled backdrop).

### Game Loop
- **Explicit tick loop** — Fixed timestep with update/render split — more control for animations. Must be deterministic across different hardware speeds (not tied to system clock).

### Determinism
- **Fixed-point math** — Use fixed-point arithmetic for all game logic — guarantees identical behavior across Desktop and Android

### Character Expressions & Animation
- **Both approaches supported**:
  - Pre-rendered full sprite images (e.g., PNG with blush included)
  - Layered compositing (body, eyes, mouth, blush as separate layers)
- **Layered animation**: Allow animated repetitive character actions (e.g., playing with hair) within a single scene using layered composition.

### Minigames / Interactive Elements
- **Screen system** — Reusable UI overlays defined separately from scenes (like Ren'Py's Screens). Users can create custom interactive screens.

### Build/Packaging
- **Gradle plugin** — Custom Gradle task that bundles engine + assets into a distributable game

### Gradle Plugin API
- **Simple extension** — `vnEngine { gameName = "..."; mainScene = "..." }` — minimal config

### Asset Formats
- **Standard + optional compression** — Support standard formats (PNG/OGG/WAV), add optional compression for distribution

### Distribution
- **Gradle/Maven dependency** — Publish as a library — users add it as a dependency, like any Kotlin lib

### Branching
- **Kotlin functions + labels** — Scenes as functions, branching via if/when — natural Kotlin control flow

### Testing & Visualization
- **All three approaches supported**, let users choose:
  1. Compose Preview (@Preview) to render individual scenes in IDE
  2. Headless scene runner for automated testing with mocked state
  3. Visual scene editor (drag-and-drop builder)

### Plugins/Mods
- **No, keep it closed** — Simpler architecture, faster to ship

### Project Structure
- **Single module** — Everything in one Gradle module — simplest, fastest to start

### MVP Scope
- **Core loop + save/load + audio** — Dialogue, sprites, choices, save/load, and audio (BGM/SFX/voice)

### Performance Target
- **60 FPS on mid-range hardware** — Smooth animations on typical laptops

### Timeline
- **4-6 months** — Part-time or careful pace — more room for polish and testing

### Milestones
- **Feature-based** — M1: Core loop, M2: Save/load, M3: Audio, M4: Effects, M5: Polish, M6: Android

### Documentation
- **README + samples** — Simple README with a few example scenes — fastest to create

### Versioning
- **Semantic versioning** — Breaking changes only in major versions, clear contracts

### Accessibility
- **Built-in from start** — Design with accessibility in mind from day one — more inclusive, more work

---

## Progress Log

- **M1 (Core loop)** ✅ — Scene DSL (game/scene/show/dialogue/timeline), GameEngine with StateFlow, Compose renderer, click-to-advance
- **M2 (Save/load)** ✅ — SaveSystem (kotlinx.serialization JSON snapshot), SaveSlotManager, forward-compatible schema
- **M3 (Audio)** ✅ — expect/actual AudioManager (javax.sound.sampled on desktop), DSL cues bgm/sfx/voice, wired into engine
- **M4 (Effects)** ✅ — Timeline DSL (fade/dissolve/slide/move/shake/flash/zoom/wait/parallel), EffectRunner scheduler, 60fps tick loop, per-sprite alpha/offset/rotation/scale
- **M5 (Polish)** ✅ — Typewriter dialogue (click to complete→advance), speaker name plate, named/hex backgrounds, flash + fade overlays, 1280x720 window, real sprites
- **M6 (Android)** ⏳ — KMP Android target not started
- **M6 (Animation)** ✅ — Ported review-tool pack system: `character.json` pack + `Animator` (frame timings, loop) in commonMain; `show(id, at, anim = "idle")` DSL; frameSrc-driven rendering with bitmap cache; face-zoom stays active
- **M7 (Android)** ⏳ — KMP Android target not started
