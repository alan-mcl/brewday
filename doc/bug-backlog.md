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

### B8: Running JUnit tests pops up various error dialogs
JUnit should run headless and not pop up any dialogs.

## Resolved

- **B2** (closed): `InventoryScreen` table uses a dirty-aware cell renderer (`isRowDirty` + `modelLineItems`) so edited or added line items render bold; covered by `InventoryScreenTest.dirtyRowsAreBold`.
- **B3** (closed): Main window uses `DO_NOTHING_ON_CLOSE` with `requestApplicationExit()`: if `DirtyStateService.hasDirty()`, shows confirm (`editor.discard.all.msg` / `ui.exit`) before `System.exit(0)`; quit hotkey (Ctrl/Cmd+Shift+Q) uses the same path. Tests use `TestableSwingAppFrame` hooks instead of exiting the JVM.
- **B7** (closed): `ProcessTemplatesScreen` now has **Export CSV** (toolbar + Alt+X, Ctrl/Cmd+X; UTF-8 CSV with columns `Name`, `Steps`, rows in current table view order).

