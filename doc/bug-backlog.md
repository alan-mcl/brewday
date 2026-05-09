# Bug Backlog

Shared backlog for bugs and follow-up fixes that agents can pick up in future sessions.

## How to Use

- Add newly discovered bugs as soon as they are confirmed.
- Keep entries concise and reproducible.
- Update status when work starts/finishes.
- Link to files/symbols, tests, or docs where relevant.
- If a bug is fixed, move it to the "Resolved" section with the fix date.

## Running Swing UI tests (headless / CI)

Swing/EWT tests need a working AWT display. On Linux without a physical display (including many CI agents), run the Ant test target under a virtual framebuffer, for example:

`xvfb-run -a ant test`

Without `DISPLAY` or Xvfb, the JVM may fail to initialize the toolkit before test classes can skip via `GraphicsEnvironment.isHeadless()` (see **B1**).

## Priority Guide

- `P0` Critical: crashes, data loss/corruption, broken save/load.
- `P1` High: major feature blocked, severe incorrect behavior.
- `P2` Medium: user-visible bug with viable workaround.
- `P3` Low: minor issue, polish, non-blocking inconsistency.

## Open Bugs

| ID | Sev | Area     | Title                                                                                                     | 
|----|-----|----------|-----------------------------------------------------------------------------------------------------------|
| B1 | P4  | Swing UI | Swing test suite requires X11 display in headless environments (use `xvfb-run` / see **Running Swing UI tests** above) |
| B2 | P2  | Swing UI | Recipe Editor: Mash panel: tabbing to "Duration" navigates back to the Recipe Pane                        |
| B3 | P2  | Swing UI | Recipe Editor: Mash panel: Editing Grain temp doesn't refresh the recipe outcomes (e.g. mash temp)        |
| B4 | P2  | Swing UI | Recipe Editor: Misc Addition panel: "Error invalid - Litres" out of some Select And Edit Quantity widgets. |
| B5 | P2  | Swing UI | Recipe Editor: Ferment panel: ticking "remove trub and chiller loss" doesn't refresh the recipe outcome.  |


## Resolved

Move fixed items here with short closure notes:

| ID | Resolved On | Notes |
|---|---|---|
| BUG-002 | 2026-05-08 | Reference DB edit dialogs now use `SwingQuantityEditWidget` so display and parse always use the same `Quantity.Unit` (e.g. `PERCENTAGE_DISPLAY`); added widget + dialog round-trip tests. |
| BUG-003 | 2026-05-08 | Same widget migration: OG/FG use `SPECIFIC_GRAVITY`, fermentable colour initially used `LOVIBOND` in the widget; superseded for Swing Reference DB by BUG-004 (SRM normalization). |
| BUG-004 | 2026-05-08 | Swing Reference Data: fermentable colour was shown as Lovibond (`°L`) while `ColourUnit` stores SRM internally (`FermentablesScreen.fmtLovibond` used `value.get()` + `LOVIBOND` suffix). `EditFermentableDialog` also edited colour in Lovibond. Fixed: table uses `fmtSrm` + header key `fermentable.colour.column`; dialog uses `Quantity.Unit.SRM`. Tests: `colourSrmRoundTripWithoutEdit`, `SwingQuantityEditWidgetTest.srmRoundTrip`. |
| B6 | 2026-05-09 | Ingredient-add and mash-tool dialogs take `java.awt.Window` parents (`JDialog`/`RecipeEditorDialog`); removed `(Frame)`/`(JFrame)` casts from `SwingIngredientAdditionPane`, `SwingProcessStepPane`, mash panes. Base `SwingIngredientAdditionDialog` uses `Dialog.ModalityType.APPLICATION_MODAL`. Test: `SwingHopAdditionDialogTest.hopAdditionDialogAcceptsJDialogOwnerWindow`. |
