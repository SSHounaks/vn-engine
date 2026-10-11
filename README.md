# vn-engine

A Kotlin Multiplatform visual novel engine built on Compose Multiplatform. A type-safe,
scene-driven engine with branching stories, mood variables, save/load, audio, layered
effects, and animated sprites and backgrounds.

> **Status:** the engine core and desktop host are complete. Story content, character
> sprites, and backgrounds are intentionally **not** shipped in this open-source tree yet.

## Features

- **Type-safe scene DSL** — scenes, dialogue, choices, and timelines are declared in
  Kotlin with compile-time checks.
- **Branching & mood variables** — `VarKey<T>` typed variables (`varInt`, `varBool`,
  `varString`) with defaults; options can be gated on predicates and mutate vars on
  selection.
- **Save / load** — scene state, variables, and dialogue progress serialize via
  kotlinx.serialization.
- **Audio** — ambient music and sound effects through `javax.sound.sampled`.
- **Effects system** — fades, zooms, shakes, slides, flashes; layered, deterministic
  tick loop.
- **Animated sprites** — sprite packs with frame groups and per-group animations, plus
  static sprite fallbacks.
- **Mobile-first input model** — deterministic updates decoupled from frame rate.

## Requirements

- JDK 21
- Gradle 9.x (Kotlin Multiplatform / Compose Multiplatform toolchain)

## Build & run

```bash
gradle run          # run the desktop host
gradle createDistributable   # produce a self-contained app bundle
gradle packageDistributionForCurrentOS   # installer / archive
```

`createDistributable` outputs a runnable bundle under
`build/compose/binaries/main/app/`.

## Project structure

```
src/
  commonMain/   # engine: DSL, scene script, variables, save/load, audio, effects, tools
  desktopMain/  # desktop host: Compose renderer, sprite/background loader, main window
  commonTest/   # unit tests
```

## License

[Apache-2.0](LICENSE)

© SSHounaks
