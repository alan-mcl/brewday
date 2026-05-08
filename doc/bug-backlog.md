# Bug Backlog

Shared backlog for bugs and follow-up fixes that agents can pick up in future sessions.

## How to Use

- Add newly discovered bugs as soon as they are confirmed.
- Keep entries concise and reproducible.
- Update status when work starts/finishes.
- Link to files/symbols, tests, or docs where relevant.
- If a bug is fixed, move it to the "Resolved" section with the fix date.

## Priority Guide

- `P0` Critical: crashes, data loss/corruption, broken save/load.
- `P1` High: major feature blocked, severe incorrect behavior.
- `P2` Medium: user-visible bug with viable workaround.
- `P3` Low: minor issue, polish, non-blocking inconsistency.

## Open Bugs

| ID | Priority | Area | Title | Repro / Evidence | Suggested Fix Direction | Status | Owner |
|---|---|---|---|---|---|---|---|
| BUG-001 | P2 | Swing UI / tests | Swing test suite requires X11 display in headless environments | `ant test` fails with `Can't connect to X11 window server using ':0'` in CI/headless runs | Configure test harness for headless Swing execution or gate UI tests behind display detection | Open | Unassigned |
| BUG-002 | P1 | Swing UI / Reference DB dialogs | Percentage fields shrink on re-save due to display/internal unit mismatch | In multiple dialogs, existing percentage values are rendered with `value.get()` (internal `[0..1]`) but parsed with `Quantity.Unit.PERCENTAGE_DISPLAY` (`[0..100]`): `EditHopDialog.percent()`/`parsePercent()`, `EditYeastDialog.percent()`/`parsePercent()`, `EditMiscDialog.percent()`/`parsePercent()`, `EditFermentableDialog.percent()`/`parsePercent()`, `EditStyleDialog.percent()`/`parsePercentOrShowError()`. Example: stored `0.05` (5%) displays as `0.05`; saving parses as `0.05%` => `0.0005` internal. | Render percentages using display units (`get(Quantity.Unit.PERCENTAGE_DISPLAY)`), keep parse in `PERCENTAGE_DISPLAY`, and add round-trip tests for open-with-existing-value then save-without-edit. | Open | Unassigned |
| BUG-003 | P1 | Swing UI / Reference DB dialogs | OG/FG and fermentable colour fields use wrong display transform before parsing | `EditStyleDialog.density()` uses `DensityUnit.get()` (GU) but parse expects `SPECIFIC_GRAVITY`; existing OG like 1.050 displays as ~50 then re-saves as SG 50 (massively wrong). `EditFermentableDialog.lovibond()` uses `ColourUnit.get()` (SRM) but parse expects `LOVIBOND`, causing unit drift on re-save. | Render with matching units used by parser (`get(Quantity.Unit.SPECIFIC_GRAVITY)` for style OG/FG and `get(Quantity.Unit.LOVIBOND)` for fermentable colour), then add dialog round-trip tests to prevent regressions. | Open | Unassigned |

## Resolved

Move fixed items here with short closure notes:

| ID | Resolved On | Notes |
|---|---|---|

