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
test classes can skip via `GraphicsEnvironment.isHeadless()` (see **B1**).

## Priority Guide

- `P0` Critical: crashes, data loss/corruption, broken save/load.
- `P1` High: major feature blocked, severe incorrect behavior.
- `P2` Medium: user-visible bug with viable workaround.
- `P3` Low: minor issue, polish, non-blocking inconsistency.

## Open Bugs

### B1: Swing test suite requires X11 display in headless environments
P4 / Swing UI / Open

Swing/EWT tests require an AWT display in headless environments. Use
`xvfb-run` and see **Running Swing UI tests** above.

### B5: Ferment trub/loss toggle does not refresh recipe outcome
P2 / Swing UI / Open

In the Recipe Editor Ferment panel, ticking "remove trub and chiller loss" does
not refresh the recipe outcome.

### B7: JavaFX Brewing Settings IBU listeners persist wrong values — CLOSED (UI removed)
P2 / was JFX UI / Closed

The JavaFX UI has been removed. The issue affected **`BrewingSettingsIbuPane`** (deleted).

Swing **`BrewingSettingsIbuScreen`** carries the corrected listener wiring. No further JFX fix planned.

### B9: Data-table toolbar Save All and Undo All run on the EDT
P3 / Swing UI / Open

Toolbar **Save All** / **Undo All** on individual data-table screens still run
**`Database#saveAll` / `#loadAll`** on the EDT, so a large DB may freeze briefly.

**`SwingAppFrame`** global shortcuts moved these calls to **`SwingWorker`**.

Optional follow-up: route toolbar actions through the same async path or a
shared service.

