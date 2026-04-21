# AGENTS.md

## Scope

Guidance for AI agents modifying this repository. Follow observed project patterns; do not import external style systems.

## How to Approach Changes

- Start by locating the layer(s) touched by the request:
  - UI: `src/main/java/mclachlan/brewday/ui/jfx`
  - Domain/process/math: `src/main/java/mclachlan/brewday/recipe`, `process`, `math`
  - Persistence: `src/main/java/mclachlan/brewday/db`, `db/v2`
  - Import/export: `src/main/java/mclachlan/brewday/importexport`
- Trace fan-out before editing:
  - Enum/type changes often require serializer + UI + domain updates.
  - Persisted model changes require serializer + `Database` wiring + data compatibility handling.
- Keep edits minimal and local; avoid broad rewrites.

## Dominant Conventions to Preserve

- Core Java style uses tabs and Allman braces.
- Naming is consistent: PascalCase classes, lowerCamelCase members, UPPER_SNAKE_CASE constants.
- Core files often use method separators `/*-------------------------------------------------------------------------*/`.
- Singleton access is a common pattern (`Brewday`, `Database`, `JfxUi`, `DocumentCreator`).

## What Not to Change Casually

- Persisted JSON keys and file names in `Database`/serializers.
- `ProcessStep.Type` and `IngredientAddition.Type` mappings without full cross-layer updates.
- Backup/restore flow in `Database.saveAll()` and `loadAll()` behavior.
- Global singleton lifecycle and initialization order.
- Build/distribution assumptions in `build.xml` unless task explicitly requests packaging changes.

## Safe Extension Playbooks

### Add a new process step

- Update type enum in process model.
- Add serializer read/write mapping in `db/StepSerialiser`.
- Wire creation/editing in JavaFX (`NewStepDialog`, `RecipeEditor`, related pane classes).
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

