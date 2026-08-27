# Bug Backlog

Shared backlog for bugs and follow-up fixes that agents can pick up in future 
sessions.

## How to Use

- Add newly discovered bugs as soon as they are confirmed.
- Keep entries concise and reproducible.
- Update status when work starts/finishes.
- Link to files/symbols, tests, or docs where relevant.

## Running Swing UI tests (headless / CI)

Swing/EWT tests need a working AWT display. On Linux without a physical display 
(including many CI agents), run the Ant test target under a virtual framebuffer, 
for example:

`xvfb-run -a ant test`

Without `DISPLAY` or Xvfb, the JVM may fail to initialize the toolkit before 
test classes can skip via `GraphicsEnvironment.isHeadless()`.

## Priority Guide

- `P0` Critical: crashes, data loss/corruption, broken save/load, materially wrong calculations.
- `P1` High: major feature blocked, severe incorrect behavior, significant import/export gaps.
- `P2` Medium: user-visible bug with viable workaround, process edge cases, incomplete modelling.
- `P3` Low: minor issue, polish, non-blocking inconsistency, likely dead code or test-only.

## Backlog

Status values: `Open`, `In Progress`, `Done`. Update status when work starts or finishes.

| ID  | Pri | Type      | Summary                                                              | Location                                                  | Status   | Notes                                                                                                                                                                    |
|-----|-----|-----------|----------------------------------------------------------------------|-----------------------------------------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| T1  | P3  | TODO      | BeerXML import: BIAB mash infusion volume logic                      | `importexport/beerxml/BeerXmlParser.java` ~490            | Open     |                                                                                                                                                                          |
| T3  | P3  | TODO      | BeerXML import: infusion water temp not adjusted to hit step target  | `BeerXmlParser.java` ~497, 540, 572                       | Open     | Three call sites                                                                                                                                                         |
| T12 | P3  | TODO      | BeerXML import: yeast `times_cultured` and `max_reuse`               | `BeerXmlRecipesHandler.java` ~1116, 1120                  | Open     | Metadata not imported                                                                                                                                                    |
| T15 | P2  | TODO      | Mash infusion: validate two-fluid temperature model                  | `process/MashInfusion.java` ~110                          | Open     | Research whether approach is valid                                                                                                                                       |
| T17 | P2  | TODO      | Heating volume change not modelled                                   | `process/Heat.java` ~96                                   | Open     |                                                                                                                                                                          |
| T23 | P3  | TODO      | Pass `ProcessLog` into `sortSteps`                                   | `process/Volumes.java` ~282                               | Open     | Better diagnostics during sort                                                                                                                                           |
| T24 | P3  | TODO      | Remove or implement dead `RecipeHandler.handleRecipe` stubs          | `BeerXmlRecipesHandler.java` ~314–366                     | Open     | `style`, `equipment`, `hops`, etc. handled via sub-handlers                                                                                                              |
| T25 | P3  | TODO      | `GoogleDriveBackend` empty todo                                      | `test_data/gdrive/GoogleDriveBackend.java` ~215           | Open     | Test/support code only                                                                                                                                                   |
| H1  | P2  | Tech debt | Mash hop utilisation pass-through hack                               | `process/Mash.java` ~210                                  | Open     | Related: `Lauter.java` ~127                                                                                                                                              |
| H2  | P2  | Tech debt | Lauter utilisation pass-through hack                                 | `process/Lauter.java` ~127                                | Open     |                                                                                                                                                                          |
| H3  | P3  | Tech debt | Volume-type alignment hack                                           | `process/FluidVolumeProcessStep.java` ~45                 | Open     |                                                                                                                                                                          |
| H4  | P3  | Tech debt | Volume→weight conversion hack in equations                           | `math/Equations.java` ~1368                               | Open     |                                                                                                                                                                          |
| H5  | P3  | Tech debt | BeerXML handler workarounds                                          | `BeerXmlRecipesHandler.java` ~120, 128                    | Open     | Commented as hacks in source                                                                                                                                             |
| F7  | P2  | TODO      | Ferment: open vs closed vessel, spunding, bottle conditioning        | `process/Ferment.java`, `PackageStep`                     | Open     | Model CO₂ generation and headspace during ferment phases; current model sets equilibrium once then preserves across chained phases                                         |
| F2  | P3  | TODO      | No-fermentation phases may discard dormant yeast cultures            | `FermentationCalculator`, `FermentationResult`            | Open     | Distinguish "no attenuation" from "no surviving cultures"; cold conditioning and aging phases should preserve cultures                                                   |
| F3  | P3  | TODO      | Activity-state transitions too simplistic                            | `FermentationCalculator`                                  | Open     | Current heuristics over-rely on attenuation completion and temp stress; later refine using activity rate, viability, flocculation and alcohol tolerance                  |
| F4  | P3  | TODO      | Generation numbering semantics may be off-by-one                     | `YeastCulture`, `FermentationCalculator`                  | Open     | Decide whether fresh commercial pitches are Generation 0 or Generation 1 before implementing starter/slurry workflows                                                    |
| F5  | P3  | TODO      | Blend attenuation weighting may over-favor dominant cultures         | `FermentationCalculator`                                  | Open     | Current weighting uses `effectiveCells^1.2`; later consider softer weighting or phase/sugar-class-aware contribution models                                              |
| F6  | P2  | TODO      | Fermentation progress inferred only from attenuation outcome         | `FermentationCalculator`, `Volume` fermentation metrics   | Open     | Future model should explicitly track remaining fermentable extract or attenuation potential across chained phases                                                        |


