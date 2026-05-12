# AGENTS.md

## Scope

Guidance for AI agents modifying this repository. Follow observed project patterns; do not import external style systems.

## How to Approach Changes

- consult the specifications at
  - [design-document.md](doc/design-document.md)
  - [data-model-document.md](doc/data-model-document.md)
- consult and maintain the shared bug backlog at
  - [bug-backlog.md](doc/bug-backlog.md)
- Start by locating the layer(s) touched by the request:
  - UI (Swing): `src/main/java/mclachlan/brewday/ui/swing` (`app`, `screens`, `dialogs`, `widgets`)
  - Domain/process/math: `src/main/java/mclachlan/brewday/recipe`, `process`, `math`
  - Persistence: `src/main/java/mclachlan/brewday/db`, `db/v2`
  - Import/export: `src/main/java/mclachlan/brewday/importexport`
- Trace fan-out before editing:
  - Enum/type changes often require serializer + UI + domain updates.
  - Persisted model changes require serializer + `Database` wiring + data compatibility handling.
- Keep edits minimal and local; avoid broad rewrites.
- When you discover a bug that is out of scope for the current task, add it to `doc/bug-backlog.md` with repro notes and priority.
- For UI changes, refer to and update the specifications in the docs folder
  - Swing UI: [swing-ui-design-spec.md](doc/swing-ui-design-spec.md)
  - Historical JavaFX notes (obsolete UI, sources removed): [jfx-ui-design-spec.md](doc/jfx-ui-design-spec.md)

## Dominant Conventions to Preserve

- Core Java style uses tabs and Allman braces.
- Naming is consistent: PascalCase classes, lowerCamelCase members, UPPER_SNAKE_CASE constants.
- Core files often use method separators `/*-------------------------------------------------------------------------*/`.
- Singleton access is a common pattern (`Brewday`, `Database`, `DocumentCreator`).

## What Not to Change Casually

- Persisted JSON keys and file names in `Database`/serializers.
- `ProcessStep.Type` and `IngredientAddition.Type` mappings without full cross-layer updates.
- Backup/restore flow in `Database.saveAll()` and `loadAll()` behavior.
- Global singleton lifecycle and initialization order.
- Build/distribution conventions in [`build.xml`](build.xml): do not refactor packaging targets casually. Tasks that intentionally change **`package-*`** / **`zipdist`** flows must stay aligned with **[`doc/packaging.md`](doc/packaging.md)** (targets, JDK expectations, Troubleshooting).

## Safe Extension Playbooks

### Add a new process step

- Update type enum in process model.
- Add serializer read/write mapping in `db/StepSerialiser`.
- Wire creation/editing in Swing (`SwingNewStepDialog`, recipe/step editors, related pane classes).
- Verify recipe run/sort behavior still succeeds.

### Add a new persisted entity

- Add domain class in the appropriate package.
- Create serializer (reflective or explicit as needed).
- Register map+silo load/save wiring in `db/Database`.
- Ensure UI CRUD pane integration if user-facing.

### Extend import/export

- Keep format-specific parsing in `importexport/*`.
- Reuse existing object creation/mapping patterns rather than embedding parser logic in UI panes.
- Validate with representative files under `test_data/beerxml` or related fixtures.

## Error Handling and Runtime Behavior

- Follow existing pattern: throw/wrap `BrewdayException` for hard failures; use process/UI logs for user-visible warnings/errors.
- Use validation-first early returns in process steps where that pattern exists.
- Do not silently ignore invalid data paths.

## Performance and Safety Constraints

- Process execution (`process/*`, `recipe/Recipe`) is computation-heavy; keep code straightforward and loop-based.
- Avoid introducing unnecessary allocations or multi-pass transforms in hot process logic.
- Treat persistence as snapshot writes over in-memory maps; avoid partial-write behavior changes unless explicitly requested.

## Validation Practice in This Repo

- Build validation is Ant-based (`build.xml`) with compile/dist targets.
- Automated tests are limited; many checks are manual harnesses in `src/main/java/mclachlan/brewday/test` and `run_*_test.cmd`.
- For UI-impacting changes, prefer targeted harness/manual checks plus compile success.

## Documentation and Phase Tracking Requirements

- When implementing or completing a Swing UI phase, you must update `doc/swing-ui-design-spec.md` in the same change set.
- Keep phase statuses in section 5 accurate:
  - move `TODO`/`In Progress` -> `Implemented` when scope is delivered,
  - do not leave completed phases marked in-progress.
- Update `Current implementation references` when new key Swing surfaces/dialogs are introduced for that phase.
- Add or update concise phase closure notes when they help explain parity-complete behavior delivered.
- For UI work, verify documentation consistency with `doc/swing-ui-design-spec.md`. Use `doc/jfx-ui-design-spec.md` only as optional historical context (JavaFX UI removed).

