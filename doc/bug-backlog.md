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
test classes can skip via `GraphicsEnvironment.isHeadless()` (see **B0**).

## Priority Guide

- `P0` Critical: crashes, data loss/corruption, broken save/load, materially wrong calculations.
- `P1` High: major feature blocked, severe incorrect behavior, significant import/export gaps.
- `P2` Medium: user-visible bug with viable workaround, process edge cases, incomplete modelling.
- `P3` Low: minor issue, polish, non-blocking inconsistency, likely dead code or test-only.

## Backlog

Status values: `Open`, `In Progress`, `Done`. Update status when work starts or finishes.

| ID  | Pri | Type      | Summary                                                              | Location                                                  | Status   | Notes                                                                                                                                                                    |
|-----|-----|-----------|----------------------------------------------------------------------|-----------------------------------------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| T1  | P0  | TODO      | BeerXML import: BIAB mash infusion volume logic                      | `importexport/beerxml/BeerXmlParser.java` ~490            | Open     |                                                                                                                                                                          |
| T2  | P0  | TODO      | BeerXML import: decoction uses wrong mash volume                     | `BeerXmlParser.java` ~599                                 | Open     | Uses equipment mash-tun volume, not actual mash volume                                                                                                                   |
| T3  | P0  | TODO      | BeerXML import: infusion water temp not adjusted to hit step target  | `BeerXmlParser.java` ~497, 540, 572                       | Open     | Three call sites                                                                                                                                                         |
| T7  | P0  | TODO      | Trub/chiller loss flag only on Ferment; should be on more step types | `process/Ferment.java` ~53–54                             | Open     | Volume transfer modelling                                                                                                                                                |
| T8  | P1  | TODO      | BeerXML import: ramp time ignored on first mash step                 | `BeerXmlParser.java` ~484                                 | Open     |                                                                                                                                                                          |
| T11 | P1  | TODO      | BeerXML import: fermentable colour Lovibond vs SRM                   | `BeerXmlRecipesHandler.java` ~1779                        | Open     | Spec mismatch; error likely small                                                                                                                                        |
| T12 | P1  | TODO      | BeerXML import: yeast `times_cultured` and `max_reuse`               | `BeerXmlRecipesHandler.java` ~1116, 1120                  | Open     | Metadata not imported                                                                                                                                                    |
| T13 | P1  | TODO      | `parseQuantity` is WIP; improve free-text quantity parsing           | `Brewday.java` ~403–413                                   | Open     | Javadoc notes work in progress                                                                                                                                           |
| T14 | P2  | TODO      | Sparge / infusion pH impact not modelled                             | `process/BatchSparge.java` ~214, `MashInfusion.java` ~149 | Open     |                                                                                                                                                                          |
| T15 | P2  | TODO      | Mash infusion: validate two-fluid temperature model                  | `process/MashInfusion.java` ~110                          | Open     | Research whether approach is valid                                                                                                                                       |
| T16 | P2  | TODO      | Mash water combination                                               | `process/Mash.java` ~304                                  | Open     |                                                                                                                                                                          |
| T17 | P2  | TODO      | Heating volume change not modelled                                   | `process/Heat.java` ~96                                   | Open     |                                                                                                                                                                          |
| T18 | P2  | TODO      | Boil ctor: find last wort volume, not any wort                       | `process/Boil.java` ~77                                   | Open     |                                                                                                                                                                          |
| T21  | P2  | TODO      | Dilute: support multiple water additions                             | `process/Dilute.java` ~84                                 | Open     |                                                                                                                                                                          |
| T23 | P3  | TODO      | Pass `ProcessLog` into `sortSteps`                                   | `process/Volumes.java` ~232                               | Open     | Better diagnostics during sort                                                                                                                                           |
| T24 | P3  | TODO      | Remove or implement dead `RecipeHandler.handleRecipe` stubs          | `BeerXmlRecipesHandler.java` ~314–366                     | Open     | `style`, `equipment`, `hops`, etc. handled via sub-handlers                                                                                                              |
| T25 | P3  | TODO      | `GoogleDriveBackend` empty todo                                      | `test_data/gdrive/GoogleDriveBackend.java` ~215           | Open     | Test/support code only                                                                                                                                                   |
| H1  | P2  | Tech debt | Mash hop utilisation pass-through hack                               | `process/Mash.java` ~172                                  | Open     | Related: `Lauter.java` ~116                                                                                                                                              |
| H2  | P2  | Tech debt | Lauter utilisation pass-through hack                                 | `process/Lauter.java` ~116                                | Open     |                                                                                                                                                                          |
| H3  | P3  | Tech debt | Volume-type alignment hack                                           | `process/FluidVolumeProcessStep.java` ~45                 | Open     |                                                                                                                                                                          |
| H4  | P3  | Tech debt | Volume→weight conversion hack in equations                           | `math/Equations.java` ~885                                | Open     |                                                                                                                                                                          |
| H5  | P3  | Tech debt | BeerXML handler workarounds                                          | `BeerXmlRecipesHandler.java` ~120, 128                    | Open     | Commented as hacks in source                                                                                                                                             |
| F1  | P2  | TODO      | Carbonation model resets to equilibrium every Ferment phase          | `process/Ferment.java`, `FermentationCalculator`          | Open     | Preserve incoming carbonation across chained fermentation phases; later support open vs closed fermentation, spunding, bottle conditioning                               |
| F2  | P3  | TODO      | No-fermentation phases may discard dormant yeast cultures            | `FermentationCalculator`, `FermentationResult`            | Open     | Distinguish "no attenuation" from "no surviving cultures"; cold conditioning and aging phases should preserve cultures                                                   |
| F3  | P3  | TODO      | Activity-state transitions too simplistic                            | `FermentationCalculator`                                  | Open     | Current heuristics over-rely on attenuation completion and temp stress; later refine using activity rate, viability, flocculation and alcohol tolerance                  |
| F4  | P3  | TODO      | Generation numbering semantics may be off-by-one                     | `YeastCulture`, `FermentationCalculator`                  | Open     | Decide whether fresh commercial pitches are Generation 0 or Generation 1 before implementing starter/slurry workflows                                                    |
| F5  | P3  | TODO      | Blend attenuation weighting may over-favor dominant cultures         | `FermentationCalculator`                                  | Open     | Current weighting uses `effectiveCells^1.2`; later consider softer weighting or phase/sugar-class-aware contribution models                                              |
| F6  | P2  | TODO      | Fermentation progress inferred only from attenuation outcome         | `FermentationCalculator`, `Volume` fermentation metrics   | Open     | Future model should explicitly track remaining fermentable extract or attenuation potential across chained phases                                                        |

