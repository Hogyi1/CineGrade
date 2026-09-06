# CineGrade — Architecture & Development Plan

> **Time budget:** ~4.5 weeks × 5h/day ≈ **110 hours** (core desktop) + ~2.5 weeks for social/web integration  
> **Target:** Java 21 · JavaFX 23 · LWJGL 3.4 · Maven  
> **Thesis constraint:** No code generation — this plan is a collaborative blueprint for hand-written code.

---

## 1. Package Structure

All source code lives under the root package `hu.elte.ik.thesis.cinegrade`.  
Follows a **layered architecture** consistent with the author's prior project (`com.hogyo.videocompressor`).

```
hu.elte.ik.thesis.cinegrade
│
├── app                              # APPLICATION LAYER — bootstrap, orchestrators, persistence
│   ├── main                             # Application entry point & startup
│   │   ├── App.java                         # extends Application — JavaFX main entry
│   │   ├── Launcher.java                    # main() — workaround for JavaFX module system
│   │   └── RequirementChecker.java          # Pre-flight: DLLs, tools, dirs, resources
│   ├── managers                         # Business logic orchestrators (stateful singletons)
│   │   ├── catalog                          # CatalogManager — create/open/delete/list catalogs
│   │   ├── files                            # FileOrganizer — move photos, export hierarchy to OS
│   │   ├── settings                         # SettingsManager — load/save/reset app & LibRaw settings
│   │   ├── tasks                            # TaskManager — submit/track/cancel async jobs
│   │   └── editing                          # EditingSession — active photo + EditState + undo/redo
│   └── repositories                     # Data persistence (JSON ↔ disk)
│       ├── catalog                          # CatalogSerializer — Gson-based catalog manifest I/O
│       ├── presets                           # PresetLibrary — preset & LUT collection persistence
│       └── settings                         # Settings JSON file read/write
│
├── domain                           # DOMAIN LAYER — pure data, no dependencies
│   ├── enums                            # ErrorCode, TaskType, Rating, ExportFormat, etc.
│   ├── exceptions                       # CineGradeException, NativeToolException, CatalogException, etc.
│   ├── catalog                          # Catalog, CatalogFolder, PhotoEntry, PhotoMetadata
│   ├── editing                          # EditState, SliderParameter, CurvePoints, ColorWheel
│   ├── presets                          # Preset, LutFile
│   ├── export                           # ExportSettings
│   └── results                          # ProcessResult, RawImageData
│
├── infra                            # INFRASTRUCTURE LAYER — external tool wrappers
│   ├── config                           # AppSettings, LibRawSettings (POJOs for Gson)
│   ├── logger                           # Log4j setup / utility wrapper
│   ├── process                          # ProcessRunner — generic CLI wrapper (ProcessBuilder)
│   └── services                         # 3rd-party tool services
│       ├── libraw                           # LibRawLibrary (JNA interface), LibRawService
│       ├── exiftool                         # ExifToolService — metadata extract/write via CLI
│       ├── ffmpeg                           # FFmpegService — image encoding/export
│       └── ffprobe                          # FFprobeService — file validation/codec info
│
├── engine                           # RENDERING LAYER — LWJGL / OpenGL GPU pipeline
│   ├── RenderEngine.java               # Init OpenGL context, manage render-on-demand loop
│   ├── FrameBufferManager.java          # Double-buffered FBO setup & swap
│   ├── ShaderProgram.java               # Compile/link GLSL, set uniforms
│   ├── ColorPipelineShader.java         # Main grading shader (uniforms ← EditState)
│   ├── TextureManager.java              # Upload decoded pixels → GPU texture
│   └── ViewportController.java          # Zoom, pan, crop tool interaction on the canvas
│
├── tasks                            # TASK LAYER — async job implementations
│   ├── CineTask.java                   # Abstract base — extends javafx.concurrent.Task<T>
│   ├── ImportPhotoTask.java             # Pipeline: copy → thumbnail → EXIF → register
│   ├── ExportPhotoTask.java             # Pipeline: bake edits → encode → write to disk
│   ├── ExportLutTask.java               # Generate .cube file from current EditState
│   ├── BatchExportTask.java             # Wraps multiple ExportPhotoTasks with progress
│   ├── ThumbnailGenerationTask.java     # Background thumbnail extraction for file browser
│   └── MetadataExtractionTask.java      # Background EXIF extraction via ExifToolService
│
└── javafx.ui                    # UI LAYER — JavaFX views & controllers
    ├── MainController.java          # Top-level navigation (File / Edit / Export pages)
    ├── StartScreenController.java   # Recent catalogs list, create / open actions
    ├── file
    │   ├── FilePageController.java  # Grid/list of photos, drag-and-drop import
    │   ├── FolderTreeController.java# Left sidebar: virtual folder tree
    │   └── PhotoThumbnailCell.java  # Custom cell renderer: thumbnail + rating stars
    ├── edit
    │   ├── EditPageController.java  # Orchestrates carousel + viewport + panels
    │   ├── CarouselController.java  # Bottom strip: thumbnail navigation + filtering
    │   ├── ViewportPane.java        # Hosts the LWJGL-rendered ImageView + overlays
    │   ├── SliderPanelController.java  # Right panel: exposure, WB, HSL sliders
    │   ├── CurvePanelController.java   # Right panel: interactive curve editor
    │   ├── ColorWheelController.java   # Right panel: 3-way color wheel widget
    │   ├── PresetPanelController.java  # Left panel: preset/LUT library browser
    │   └── BeforeAfterOverlay.java     # Draggable split comparison overlay
    ├── export
    │   ├── ExportPageController.java   # Batch config, queue monitor, progress bars
    │   └── ExportItemCell.java         # Custom cell: per-photo export settings
    ├── settings
    │   └── SettingsDialogController.java  # Modal: app settings + LibRaw settings
    └── components                   # Reusable JavaFX components
        ├── StarRatingControl.java       # 1-5 star rating widget
        ├── TaskProgressBar.java         # Bindable progress bar tied to CineTask
        └── ConfirmationDialog.java      # Reusable "Are you sure?" dialog
```

---

## 2. Error Management Strategy

### 2.1 ErrorCode Enum

Every error in the system is identified by a unique enum constant with a numeric code and human-readable message template:

```
ErrorCode Enum Fields:
  - int code          (e.g., 1001, 2001, 3001...)
  - String message    (e.g., "LibRaw DLL not found at expected path: %s")
  - Severity level    (FATAL, ERROR, WARNING)
```

**Proposed code ranges:**

| Range | Domain |
|-------|--------|
| 1000–1099 | Startup / RequirementChecker |
| 1100–1199 | Native tools (LibRaw, FFmpeg, ExifTool) |
| 2000–2099 | Catalog operations |
| 2100–2199 | File management / import |
| 3000–3099 | Editing / preset operations |
| 4000–4099 | Export pipeline |
| 5000–5099 | Rendering / OpenGL |
| 9000–9099 | Unexpected / generic |

### 2.2 Exception Hierarchy

```
CineGradeException (checked)
├── NativeToolException       — LibRaw load failure, FFmpeg not on PATH, ExifTool crash
├── CatalogException          — corrupt manifest, missing catalog dir, duplicate name
├── ImportException           — unsupported format, file locked, decode failure
└── ExportException           — disk full, invalid settings, render pipeline failure
```

Every exception carries an `ErrorCode` + optional cause. The UI layer catches these and presents user-friendly error dialogs. Log4j logs the full stack trace.

---

## 3. RequirementChecker — Pre-Flight Validation

Runs **before** the JavaFX stage is shown. Checks:

| # | Check | On Failure |
|---|-------|------------|
| 1 | `libraw.dll` exists at expected path & JNA can load it | FATAL → exit with error dialog |
| 2 | `ffmpeg` / `ffprobe` reachable on system PATH (`ffmpeg -version`) | FATAL → exit |
| 3 | `exiftool` reachable on system PATH (`exiftool -ver`) | FATAL → exit |
| 4 | App data directory is writable (`~/.cinegrade/`) | FATAL → exit |
| 5 | Required resource files present (default thumbnails, GLSL shaders, default settings JSON) | FATAL → exit |
| 6 | OpenGL 3.3+ context can be created (LWJGL check) | FATAL → exit |

Each check logs its result via Log4j. On failure, logs the `ErrorCode` and shows a non-technical error dialog with instructions.

---

## 4. Native Tool Services — Detailed Design

### 4.1 ProcessRunner (Generic)

A reusable utility that wraps `java.lang.ProcessBuilder`:

- **Input:** command + args, working directory, timeout, environment vars
- **Output:** `ProcessResult` record → `exitCode`, `stdout` (String), `stderr` (String), `durationMs`
- **Features:** configurable timeout with forced kill, stderr/stdout capture on separate threads to avoid deadlock, Log4j logging of command + exit code
- **Thread-safety:** stateless — safe to call from any thread

### 4.2 LibRawService (JNA)

- `LibRawLibrary.java` — JNA `Library` interface mapping the C functions from `libraw.dll`:
  - `libraw_init()`, `libraw_open_file()`, `libraw_unpack()`, `libraw_dcraw_process()`, `libraw_dcraw_make_mem_image()`, `libraw_close()`, `libraw_recycle()`
- `LibRawService.java` — high-level Java API:
  - `decodeToPixelBuffer(Path rawFile, LibRawSettings settings) → RawImageData`
  - `extractThumbnail(Path rawFile) → byte[]`
  - Handles JNA memory lifecycle (allocate/free), maps LibRaw error codes to `NativeToolException`

### 4.3 ExifToolService

- Uses `ProcessRunner` to call `exiftool -json <file>` → parses JSON output via Gson into `PhotoMetadata`
- Batch mode: `exiftool -json <file1> <file2> ...` for bulk extraction
- Write support: `exiftool -overwrite_original -Rating=4 <file>`

### 4.4 FFprobeService / FFmpegService

- **FFprobeService:** `validate(Path file) → boolean` — runs `ffprobe -v error -show_entries format=format_name <file>`
- **FFmpegService:** `encode(Path input, Path output, ExportSettings settings)` — builds ffmpeg command line for format conversion, quality, resize

---

## 5. Task System Design

### 5.1 Core Concepts

```
TaskManager (singleton)
├── holds: ObservableList<CineTask<?>>     ← UI can bind to this
├── submit(CineTask<?>) → registers + starts on thread pool
├── cancel(CineTask<?>) → calls task.cancel()
├── cancelAll()
└── getRunningTasks() / getCompletedTasks()

CineTask<T> (abstract, extends javafx.concurrent.Task<T>)
├── taskId: String (UUID)
├── taskType: TaskType enum (IMPORT, EXPORT, EXPORT_LUT, THUMBNAIL, METADATA, etc.)
├── createdAt: Instant
├── Provides: progress, message, state — all JavaFX-observable
└── abstract call() — subclasses implement the actual work
```

### 5.2 Task Lifecycle

```
CREATED → SCHEDULED → RUNNING → SUCCEEDED / FAILED / CANCELLED
                         ↑              │
                         └──── retry ───┘  (optional, configurable)
```

- All tasks run on a **bounded thread pool** (e.g., `Executors.newFixedThreadPool(nCores - 1)`)
- The UI binds to task progress/message properties for live feedback
- User can cancel any task via the UI → `task.cancel(true)` → task must check `isCancelled()` in its loop

### 5.3 Import Pipeline (ImportPhotoTask)

This is a **multi-step sequential task**. Steps:

1. **Copy source file** → catalog's internal storage directory
2. **Extract thumbnail** → `LibRawService.extractThumbnail()` → save as JPEG in cache
3. **Extract metadata** → `ExifToolService.extractMetadata()` → populate `PhotoMetadata`
4. **Register in catalog** → create `PhotoEntry`, add to current `CatalogFolder`, save manifest
5. **Notify UI** → fire change event so the file browser / carousel updates

Each step updates `updateProgress()` and `updateMessage()`. If any step fails, the task rolls back (deletes copied file, removes partial entry).

---

## 6. Editing System Design

### 6.1 EditState — The Central Model

`EditState` is a **plain POJO** holding every adjustable parameter for one photo:

```
EditState
├── Basic:     exposure, contrast, highlights, shadows, whites, blacks
├── WhiteBal:  temperature, tint
├── Presence:  vibrance, saturation
├── HSL:       hue/sat/lum for 8 color channels
├── Curves:    List<CurvePoints> for R, G, B, Luma
├── Wheels:    ColorWheel lift, ColorWheel gamma, ColorWheel gain
├── Crop:      cropX, cropY, cropW, cropH, rotation
└── LUT:       appliedLutPath (nullable — if an external LUT is layered on)
```

- **Serializable to JSON** via Gson → this is the "preset recipe" format
- Each `PhotoEntry` holds an `EditState` (nullable = unedited)
- `EditState.copy()` for copy/paste between photos

### 6.2 EditingSession

The active workspace state:

- `currentPhoto: PhotoEntry`
- `currentEditState: EditState` (working copy — changes are live)
- `originalPixels: RawImageData` (decoded RAW — immutable reference)
- `undoStack: Deque<EditState>` / `redoStack: Deque<EditState>`
- Methods: `pushUndo()`, `undo()`, `redo()`, `resetToOriginal()`, `applyPreset(Preset)`

### 6.3 Preset & LUT Pipeline

- **Preset** = named `EditState` snapshot saved as JSON in `~/.cinegrade/presets/`
- **LUT** = `.cube` file in `~/.cinegrade/luts/`
- **LutGenerator** takes an `EditState` and bakes it into a 3D LUT:
  - Iterates a 3D grid (e.g., 33×33×33), applies all color math to each sample, writes `.cube`
- **PresetLibrary** manages the on-disk collection, provides `ObservableList<Preset>` for UI binding

---

## 7. Rendering Pipeline (LWJGL/OpenGL)

### 7.1 Architecture

```
RenderEngine
├── Owns an off-screen OpenGL context (created via GLFW hidden window)
├── Manages the render loop on a dedicated thread
├── Double-buffered FBO: renders to back buffer, swaps to front for display
│
├── TextureManager
│   └── Uploads decoded RAW pixels as GL_TEXTURE_2D
│
├── ColorPipelineShader
│   ├── Vertex shader: full-screen quad
│   └── Fragment shader: applies ALL EditState adjustments as uniform values
│       ├── Exposure, contrast, WB → simple math
│       ├── HSL adjustments → RGB↔HSL conversion in shader
│       ├── Curves → 1D texture LUT lookup
│       ├── Color wheels → lift/gamma/gain formula (ASC-CDL style)
│       └── 3D LUT application → 3D texture lookup
│
├── FrameBufferManager
│   └── Manages FBO creation, resize, readback to JavaFX WritableImage
│
└── ViewportController
    └── Translates mouse/scroll events → zoom matrix + pan offset → uniform updates
```

### 7.2 JavaFX ↔ LWJGL Bridge

Since LWJGL renders off-screen, the result needs to get into JavaFX:

1. Render to FBO on the GL thread
2. `glReadPixels()` → `ByteBuffer`
3. Copy buffer into `javafx.scene.image.WritableImage` via `PixelWriter`
4. Display in a standard `ImageView` on the JavaFX thread

This is the "double buffering" approach — the GL thread writes one buffer while JavaFX reads the other.

> [!IMPORTANT]
> The render loop must **not** run continuously like a game. It should re-render **only** when an `EditState` parameter changes (reactive/dirty-flag approach). This saves massive GPU/CPU resources.

---

## 8. UI Page Architecture

### 8.1 Navigation Model

```
StartScreen (no catalog open)
    │
    ├── Create New Catalog → opens FilePageController
    └── Open Existing Catalog → opens FilePageController
         │
         ├── File Page (organize, import, rate, folders)
         │       └── double-click photo or "Edit" button
         │
         ├── Edit Page (main editing workspace)
         │       ├── Carousel (bottom)
         │       ├── Viewport (center — LWJGL rendered image)
         │       ├── Slider/Curve/Wheel panels (right)
         │       ├── Preset/LUT library (left)
         │       └── "Export" button → Export Page
         │
         └── Export Page (batch export queue)
                 ├── per-photo export config
                 ├── global overrides
                 └── queue monitor (progress, pause, cancel)
```

### 8.2 Settings Dialog (Modal)

- **App Settings tab:** theme, default catalog location, thread pool size, recent catalogs limit
- **LibRaw Settings tab:** demosaic algorithm, half-size decode toggle, white balance mode, output color space, output bit depth
- **Keyboard Shortcuts tab:** (optional, stretch goal)

### 8.3 Save/Load Menu

- **Save Catalog** → `CatalogSerializer.save()` — writes the JSON manifest with all photo entries, folders, edit states
- **Save As** → save to a new location
- **Auto-save** → on every destructive action (import, edit, delete) — debounced (e.g., 5s after last change)

---

## 9. Catalog Persistence Format

Each catalog is a **directory** on disk:

```
MyCatalog/
├── catalog.json          ← manifest: folder tree, photo entries, edit states
├── originals/            ← copied source RAW/JPEG files
├── thumbnails/           ← extracted JPEG thumbnails (fast loading)
└── exports/              ← default export output directory
```

`catalog.json` structure (simplified):

```json
{
  "name": "MyCatalog",
  "createdAt": "2026-09-01T12:00:00Z",
  "rootFolder": {
    "name": "All Photos",
    "children": [ { "name": "Landscapes", "children": [], "photoIds": ["uuid1"] } ],
    "photoIds": ["uuid2", "uuid3"]
  },
  "photos": {
    "uuid1": {
      "originalFileName": "DSC_0042.NEF",
      "rating": 4,
      "metadata": { "camera": "Nikon Z6", "iso": 200, ... },
      "editState": { "exposure": 0.5, "contrast": 10, ... },
      "exportSettings": [ { "format": "JPEG", "quality": 95 } ]
    }
  }
}
```

---

## 10. Development Timeline

> **Assumption:** ~5 hours/day, 7 days/week. Adjust if your schedule differs.

### Phase 1 — Foundation (Days 1–5, ~25h)

| Day | Focus | Deliverables |
|-----|-------|-------------|
| 1 | Project scaffold | Package structure, `module-info.java`, Log4j config (`log4j2.xml`), `Constants.java` |
| 2 | Error system | `ErrorCode` enum, `CineGradeException` hierarchy, logging patterns |
| 3 | Native wrappers pt.1 | `ProcessRunner`, `ExifToolService`, `FFprobeService` |
| 4 | Native wrappers pt.2 | `LibRawLibrary` (JNA interface), `LibRawService` |
| 5 | RequirementChecker | All 6 pre-flight checks, startup flow in `App.start()` |

### Phase 2 — Catalog & File System (Days 6–10, ~25h)

| Day | Focus | Deliverables |
|-----|-------|-------------|
| 6 | Catalog model | `Catalog`, `CatalogFolder`, `PhotoEntry`, `PhotoMetadata` POJOs |
| 7 | Catalog persistence | `CatalogSerializer` (Gson), `CatalogManager` (CRUD) |
| 8 | Config & settings | `AppSettings`, `LibRawSettings`, `SettingsManager`, `UserProfile` |
| 9 | Task system | `CineTask`, `TaskManager`, thread pool setup |
| 10 | Import pipeline | `ImportPhotoTask` (copy → thumbnail → EXIF → register) |

### Phase 3 — UI Shell & File Page (Days 11–15, ~25h)

| Day | Focus | Deliverables |
|-----|-------|-------------|
| 11 | Navigation shell | `MainController`, FXML layouts, page switching, Start Screen |
| 12 | File page layout | `FilePageController`, photo grid/list, `PhotoThumbnailCell` |
| 13 | Folder tree | `FolderTreeController`, create/rename/delete folders, drag-drop photos |
| 14 | File operations | Drag-drop import, delete photos, star rating (`StarRatingControl`), keyboard shortcuts |
| 15 | File export + polish | `FileOrganizer.exportHierarchy()`, confirmation dialogs, task progress display |

### Phase 4 — Editing Engine (Days 16–23, ~40h) ⚠️ Hardest phase

| Day | Focus | Deliverables |
|-----|-------|-------------|
| 16 | OpenGL bootstrap | `RenderEngine`, GLFW hidden window, basic FBO, test quad rendering |
| 17 | Shader pipeline | `ShaderProgram`, `ColorPipelineShader` (basic exposure + contrast) |
| 18 | Texture + readback | `TextureManager`, FBO → `WritableImage` bridge, display in `ViewportPane` |
| 19 | Edit model | `EditState`, `SliderParameter`, `EditingSession`, undo/redo |
| 20 | Slider panel | `SliderPanelController` — bind sliders → `EditState` → shader uniforms → re-render |
| 21 | Advanced shaders | HSL, curves (1D LUT texture), 3-way color wheels in GLSL |
| 22 | Curve + wheel UI | `CurvePanelController` (interactive bezier), `ColorWheelController` |
| 23 | Viewport tools | `ViewportController` (zoom, pan), `BeforeAfterOverlay`, copy/paste edits |

### Phase 5 — Presets, Export & Polish (Days 24–30, ~30h)

| Day | Focus | Deliverables |
|-----|-------|-------------|
| 24 | Preset system | `Preset`, `PresetLibrary`, `PresetPanelController`, save/load/apply/delete |
| 25 | LUT pipeline | `LutGenerator` (EditState → .cube), `ExportLutTask` |
| 26 | Export page | `ExportPageController`, per-photo settings, batch config, global overrides |
| 27 | Export engine | `ExportPhotoTask`, `BatchExportTask`, queue with pause/cancel, `FFmpegService` |
| 28 | Settings & save/load | `SettingsDialogController`, save/load catalog menu, auto-save |
| 29 | Carousel + filtering | `CarouselController` (thumbnail strip, arrow keys, filter by folder/rating) |
| 30 | Integration testing | End-to-end: import → edit → preset → export, bug fixes, polish |

### Phase 6 — Social / Web Integration (~2.5 weeks, separate timeline)

This phase covers the Angular SPA, Spring Boot backend, and WebView bridge — planned separately once the desktop app is solid.

---

## 11. Key Architectural Decisions & Warnings

> [!WARNING]
> ### Threading Pitfalls
> - **Never** touch JavaFX UI from non-FX threads. Always use `Platform.runLater()` for UI updates from tasks.
> - **Never** call OpenGL from the JavaFX thread. GL calls must happen on the dedicated render thread.
> - `CineTask` extends JavaFX `Task<T>` which handles `updateProgress`/`updateMessage` safely, but your `call()` body runs on the thread pool — be careful with shared state.

> [!WARNING]
> ### JNA / LibRaw Memory
> - LibRaw allocates native memory. You **must** call `libraw_close()` / `libraw_recycle()` in a `finally` block or you'll leak native memory.
> - Consider wrapping the JNA pointer lifecycle in a try-with-resources `AutoCloseable` wrapper.

> [!TIP]
> ### Render-on-Demand, Not Game-Loop
> Don't run a continuous 60fps render loop. Set a dirty flag when `EditState` changes, render once, then sleep. This is a photo editor, not a game.

> [!TIP]
> ### Gson Type Adapters
> You'll likely need custom Gson `TypeAdapter` or `JsonSerializer` for `CurvePoints` and `ColorWheel` to keep the JSON clean. Plan these early.

> [!NOTE]
> ### Module System
> Your pom.xml doesn't currently have a `module-info.java`. You'll need one for JavaFX 23 — it must `requires` javafx.controls, javafx.fxml, javafx.media, com.google.gson, and `opens` your controller packages to javafx.fxml for reflection.

---

## 12. Missing Pieces — Things You Haven't Mentioned But Will Need

| Item | Why |
|------|-----|
| **`module-info.java`** | JavaFX 23 requires it. Without it, FXML loading and reflection will fail. |
| **`log4j2.xml`** config | Log4j needs a configuration file in `src/main/resources/` to define appenders (console + file). |
| **GLSL shader files** | `.vert` and `.frag` files in `src/main/resources/shaders/` — the actual GPU programs. |
| **Default resource images** | Placeholder thumbnail, app icon, default LUT reference images. |
| **Undo/Redo system** | You mentioned editing but not undo — I've included it in `EditingSession`. Essential for any editor. |
| **Auto-save / dirty state tracking** | Users will lose work without this. Catalog should auto-save periodically. |
| **Keyboard shortcut registry** | Rating (1-5), navigation (arrows), undo (Ctrl+Z), save (Ctrl+S) — need a central handler. |
| **Drag-and-drop handlers** | JavaFX `DragEvent` handling for file import and folder organization. |

---

## 13. Dependencies Checklist (pom.xml)

Your current pom.xml already includes everything needed:

| Dependency | Status | Purpose |
|-----------|--------|---------|
| Log4j Core 2.19 | ✅ Present | Logging |
| Gson 2.12 | ✅ Present | JSON serialization |
| JNA 5.14 | ✅ Present | LibRaw DLL binding |
| JNA Platform 5.14 | ✅ Present | Platform-specific JNA utils |
| JavaFX Controls 23 | ✅ Present | UI components |
| JavaFX FXML 23 | ✅ Present | FXML loading |
| JavaFX Media 23 | ✅ Present | Media playback (if needed) |
| LWJGL 3.4.1 (core, glfw, opengl, stb) | ✅ Present | GPU rendering |
| `libraw.dll` | ✅ Present (project root) | RAW decoding |

> [!NOTE]
> You may want to add **Log4j API** (`log4j-api`) as a separate dependency alongside `log4j-core`. Best practice is to code against the API artifact and have core as the runtime implementation. Also consider adding `log4j-slf4j2-impl` if any transitive dependencies use SLF4J.

---

## 14. Summary: Class Count Estimate

| Package | Classes | Complexity |
|---------|---------|------------|
| `app` | 3 | Low |
| `config` | 4 | Low |
| `exception` | 6 | Low |
| `native` | 6 | Medium |
| `catalog` | 6 | Medium |
| `task` | 8 | Medium |
| `editing` | 10 | High |
| `rendering` | 6 | **Very High** |
| `ui` | 16 | High |
| `util` | 4 | Low |
| **Total** | **~69 classes** | — |

This is a substantial but achievable scope for ~110 hours of focused work. The rendering pipeline (Phase 4) is by far the riskiest and most time-consuming part — prioritize getting a basic working shader before adding advanced features.
