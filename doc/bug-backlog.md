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

## Resolved

Move fixed items here with short closure notes:

| ID | Resolved On | Notes |
|---|---|---|
| BUG-002 | 2026-05-08 | Reference DB edit dialogs now use `SwingQuantityEditWidget` so display and parse always use the same `Quantity.Unit` (e.g. `PERCENTAGE_DISPLAY`); added widget + dialog round-trip tests. |
| BUG-003 | 2026-05-08 | Same widget migration: OG/FG use `SPECIFIC_GRAVITY`, fermentable colour initially used `LOVIBOND` in the widget; superseded for Swing Reference DB by BUG-004 (SRM normalization). |
| BUG-004 | 2026-05-08 | Swing Reference Data: fermentable colour was shown as Lovibond (`°L`) while `ColourUnit` stores SRM internally (`FermentablesScreen.fmtLovibond` used `value.get()` + `LOVIBOND` suffix). `EditFermentableDialog` also edited colour in Lovibond. Fixed: table uses `fmtSrm` + header key `fermentable.colour.column`; dialog uses `Quantity.Unit.SRM`. Tests: `colourSrmRoundTripWithoutEdit`, `SwingQuantityEditWidgetTest.srmRoundTrip`. |

