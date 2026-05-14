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

### B0: Running JUnit tests pops up various error dialogs
JUnit should run headless and not pop up any dialogs.

### B1: Batch dialog doesn't follow the same paradigm as the Recipe dialog
The Batch dialog should follow the same paradigm as the Recipe dialog i.e.
operate on a temporary cloned Batch and only commit on user close, dropping 
changes on cancel.

### B2: Batch dialog: Batch Notes and Analysis layout is wrong.
The 'Batch Notes' and 'Analysis' sections in the Batch dialog are not properly
laid out. The layout should be vertical and left-justified with each label just
above it's related text area i.e 'Batch Notes' then right below that the editable notes text area,
the 'Analysis' and then right below that the read-only analysis text area.
The two text areas are equal size and are the elements that expand to fit space.

### B3: Batch dialog: editing measurements doesn't update the batch analysis
When editing measurements in the Batch dialog, the batch analysis is not 
updated to reflect the changes. The batch analysis should be automatically 
recalculated and updated whenever measurements are edited.

### B5: Batch dialog: measurements that are edited should show as dirty
When editing measurements in the Batch dialog, the edited measurements should be
marked as dirty (and thus displayed in bold).


