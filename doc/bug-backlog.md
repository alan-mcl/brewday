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
Swing/EWT tests require an AWT display in headless environments. Use
`xvfb-run` and see **Running Swing UI tests** above.

### B3: Batch edit dialog: "Consume Inventory" flags the entire Brewing tree as dirty
Consume Inventory should only flag Inventory as dirty, as well as all inventory
elements that have modified quantities. The entire Brewing tree should not be 
marked dirty as nothing has changed there, not even in the Batches tab.

### B4: Inventory table: New addition dialogs have no key accelerators
On the New Hops, New Fermentable, and other New dialogs on the inventory screen,
Enter should trigger the Add action, and Esc should trigger Cancel.

### B5: Batches screen: toolbar actions are inconsistently named
The batches screen toolbar should have "Save All" and "Undo All" like all the
other data table screens.

### B6: Filter actions should be consistently named, or missing
"Filter" action buttons on the toolbar across the data table screens should all
just be labelled "Filter", instead of "Filter batches" etc.

### B7: Process Templates screen is missing Filter and Export CSV toolbar actions
The Process Templates screen should have the same toolbar actions as the other
data table screens, including Filter and Export CSV, with hotkeys, tooltips, etc.

### B8: Inventory screen is missing Filter toolbar action
The Inventory screen should have the same toolbar actions as the other data table
screens, including Filter, with hotkeys, tooltips, etc.

### B9: Data-table toolbar Save All and Undo All run on the EDT
Toolbar **Save All** / **Undo All** on individual data-table screens still run
**`Database#saveAll` / `#loadAll`** on the EDT, so a large DB may freeze briefly.
**`SwingAppFrame`** global shortcuts moved these calls to **`SwingWorker`**.
Optional follow-up: route toolbar actions through the same async path or a
shared service.

## Resolved / closed

### B2: Batch edit dialog is not closed by escape — fixed
Escape closes `SwingBatchEditorDialog` (same as Close); root-pane binding in
`SwingBatchEditorDialog` constructor (`batchEditor.cancel`).