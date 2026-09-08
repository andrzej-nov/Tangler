# Antigravity Agent Context & Coding Guidelines

This document defines the architectural patterns, coding conventions, performance guidelines, and state management rules for small casual Android/Desktop/iOS Kotlin 2D games built with **LibGDX** and **LibKTX**. Primary deployment target platform: Android. Development platform: desktop Java.

---

## 1. Project & Module Architecture

* **Multiplatform Layout**:
  * `:core`: 100% of game logic, rendering, screen management, resources, and algorithms. No platform-specific Android/Desktop/iOS APIs in `:core`.
  * `:android`, `:desktop`, `:ios`: Thin launcher modules that instantiate and run `Main`.

---

## 2. Context Injection (Anti-Singleton Pattern)

* **Rule**: **NEVER use singletons or static global state** for game components or managers. On Android, the OS process/memory lifecycle makes static singletons prone to memory leaks, state corruption, or crash-on-resume.
* **`Context` Object**:
  * Create a single `Context(val game: Main)` instance inside `Main`.
  * `Context` holds references to all application-wide objects.
  * Pass `ctx: Context` explicitly via constructors to screens, logic classes, helpers, and renderers.

---

## 3. Screen & Lifecycle Management

* **Application Entry**:
  * `Main : KtxGame<KtxScreen>()` manages screens via `addScreen`, `getScreen`, `setScreen`.
  * Screens implement `KtxScreen` (`HomeScreen`, `GameScreen`, `CreditsScreen`).
* **Lifecycle Hooks**:
  * `show()`: Attach `inputProcessor`.
  * `hide()`: Detach `inputProcessor`, save progress, flush records.
  * `pause()`: Save serialized game state to `Preferences`.
  * `resume()`: Reload textures and batches (`ctx.a.reloadAtlas()`, `ctx.initBatch()`).
  * `resize(width, height)`: Recalculate layout metrics, viewport bounds, button bounds, and recreate font caches. Early exit if `width == 0 || height == 0` (meaning the window is minimized).
  * `dispose()`: Clean up fonts, batches, and native resources.

---

## 4. Rendering Pipeline & Zero-Allocation Render Loop

### Golden Rule for `render(delta: Float)`:
**Zero Object Allocations in the Render Loop.** Never instantiate objects during `render()`.
* Avoid complex calculations in `render()`.
* All game objects that are drawn on screen must clearly separate three loop phases:
  1. the behaviour, serialization and other logic; 
  2. preparations for rendering (all coordinate and color calculations, sprite loading etc.)
  3. rendering function itselfs that is called from the main `render()` entry point; ony does the drawing, following the Zero Object Allocations rule.

* **Pre-allocated Scratch Variables**:
  * Avoid creation of short-lived objects that exist only within the function scope.
  * Keep reusable instances private to classes.
  * Mutate existing instances (`set()`, `add()`, `scl()`, `sub()`) instead of returning new ones.

* **Sprite & Shape Rendering**:
  * Retain a single `PolygonSpriteBatch` in `Context` across the entire app lifecycle.
  * Use `space.earlygrey.shapedrawer.ShapeDrawer` for vector geometry (lines, rectangles, circles) backed by a 1x1 white texture (`sd = ShapeDrawer(batch, a.white)`).

* **Fonts & Typography**:
  * Generate `BitmapFont` at runtime using `FreeTypeFontGenerator` (`ADYS-Bold_V5.ttf` or similar). Set `setMaxTextureSize(2048)` for device compatibility.
  * Use `BitmapFontCache` to pre-format text during `resize()` or on score/state change, then simply call `fontCache.draw(batch)` inside `render()`.

* **Power Saving (Non-Continuous Rendering)**:
  * Default `Gdx.graphics.isContinuousRendering = false` to save battery.
  * Dynamically enable continuous rendering only when tween or animations are active.

---

## 5. Animations with Universal Tween Engine
* Use the Universal Tween Engine for short time-dependent predefined animation sequences where applicable. Manually calculate coordinates for the game objects moving where moves depend on the game logic.
* **Registration**: Register custom `TweenAccessor` implementations in `Context.init`:
  ```kotlin
  Tween.registerAccessor(FloatingTile::class.java, FloatingTileAccessor())
  Tween.registerAccessor(Sprite::class.java, SpriteAccessor())
  ```
* **Accessor Pattern**:
  * Implement `getValues()` and `setValues()` with safe null checks and direct field mutations.
* **Chaining & Sequences**:
  * Build multi-step animations using `Timeline.createSequence()`, `push()`, `pushPause()`, `beginParallel()`, and `setCallback { _, _ -> ... }`.

---

## 6. Coordinates, Grids, and Input Handling

* **Dedicated `CoordProcessor`**:
  * Centralize grid math, screen-to-board conversions, and other globally used transformations.
  * Provide `pointerPositionScreen(screenX, screenY)` and `pointerPositionField(screenX, screenY)` unprojecting via LibGDX viewports.
* **Input Architecture**:
  * Combine `GestureDetector(GestureAdapter)` (for pinch-zoom, long-press) and `InputAdapter` (for drag, tap, scroll wheel) into an `InputMultiplexer`.
  * Support desktop controls (scroll wheel zoom/pan, right-click actions) alongside mobile multi-touch.
  * Handle Android hardware Back button (`Input.Keys.BACK`) gracefully to navigate screens or prompt exit.

---

## 7. State Management & Serialization

* **Settings & Records**:
  * Store persistent preferences in `GameSettings` backed by `Gdx.app.getPreferences()`.
  * Save high scores against composite keys that include difficulty/board configuration parameters.
  ** Save game settings, high scores and current game state in separate preference keys.
* **Ultra-Compact Save Game Format**:
  * Encode game state into positional, fixed-width string formats using `StringBuilder`.
  * Every serializable logic class implements `serialize(sb: StringBuilder)` and `deserialize(s: String, i: Int): Int` (returning the next index `j`).
  * Deserialization must validate lengths and values, failing gracefully back to a fresh game if data is corrupted.

---

## 8. Kotlin Code Style & Conventions

* **Documentation**: Include KDoc comments (`/** ... */`) on classes, public/internal properties, and functions explaining purpose and lifecycle details.
* **Properties & Mutability**:
  * Prefer `val` wherever possible.
  * Use `lateinit var` with `if (this::property.isInitialized)` checks for lifecycle-bound objects (`batch`, `font`, `field`).
  * Use custom getters/setters for computed properties or preferences synchronization.
* **Idiomatic Functional Kotlin**:
  * Prefer use of the Kotlin iterators and collection filter/transform functions, chained as needed, instead of loops.
  * Use `apply` for configuring newly created objects and `with` for scoped receiver operations.
* **Safety & Fallbacks**:
  * Favor `toIntOrNull()`, `digitToIntOrNull()`, `?: return`, `ifEmpty { ... }` over unsafe casts or unhandled exceptions.
