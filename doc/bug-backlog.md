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

### B2: Modifications on the inventory screen don't mark the related rows as dirty
Adding or editing inventory items on the inventory screen doesn't mark the 
related rows as dirty.

### B3: Exiting the app while any data is dirty should prompt if they are sure
Because unsaved changes will be lost in this case.

### B8: NumberFormatException (maybe during testing?)
java.lang.NumberFormatException: For input string: "not-a-number"
at java.base/jdk.internal.math.FloatingDecimal.check(FloatingDecimal.java:2324)
at java.base/jdk.internal.math.FloatingDecimal.readJavaFormatString(FloatingDecimal.java:1928)
at java.base/jdk.internal.math.FloatingDecimal.parseDouble(FloatingDecimal.java:110)
at java.base/java.lang.Double.parseDouble(Double.java:971)
at mclachlan.brewday.math.Quantity.parseQuantity(Quantity.java:250)
at mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget.parseOrNull(SwingQuantityEditWidget.java:109)
at mclachlan.brewday.ui.swing.dialogs.EditWaterDialog.parsePpmOrShowError(EditWaterDialog.java:250)
at mclachlan.brewday.ui.swing.dialogs.EditWaterDialog.onOk(EditWaterDialog.java:198)
at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
at java.base/java.lang.reflect.Method.invoke(Method.java:565)
at mclachlan.brewday.ui.swing.dialogs.EditWaterDialogTest.invokeOnOk(EditWaterDialogTest.java:164)
at mclachlan.brewday.ui.swing.dialogs.EditWaterDialogTest.invalidPpmFocusesOffendingField(EditWaterDialogTest.java:120)
at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
at java.base/java.lang.reflect.Method.invoke(Method.java:565)
at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)
at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)
at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)
at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)
at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
at org.junit.runners.ParentRunner.run(ParentRunner.java:413)
at org.junit.runners.Suite.runChild(Suite.java:128)
at org.junit.runners.Suite.runChild(Suite.java:27)
at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)
at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
at org.junit.runners.ParentRunner.run(ParentRunner.java:413)
at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
at org.junit.runner.JUnitCore.run(JUnitCore.java:115)
at org.junit.runner.JUnitCore.runMain(JUnitCore.java:77)
at org.junit.runner.JUnitCore.main(JUnitCore.java:36)


### B9: Data-table toolbar Save All and Undo All run on the EDT
Toolbar **Save All** / **Undo All** on individual data-table screens still run
**`Database#saveAll` / `#loadAll`** on the EDT, so a large DB may freeze briefly.
**`SwingAppFrame`** global shortcuts moved these calls to **`SwingWorker`**.
Optional follow-up: route toolbar actions through the same async path or a
shared service.

## Resolved

- **B7** (closed): `ProcessTemplatesScreen` now has **Export CSV** (toolbar + Alt+X, Ctrl/Cmd+X; UTF-8 CSV with columns `Name`, `Steps`, rows in current table view order).
