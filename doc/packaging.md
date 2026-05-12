# Packaging (Swing distribution)

Shipped builds target the **Swing** UI only (`mclachlan.brewday.ui.swing.app.SwingApp`). The compile and staging classpaths use Swing-facing jars under **`lib/`** (FlatLAF, JCommon deps, etc.); JavaFX is not part of the application.

## Prerequisites

- **Full JDK 21 or newer** (not a JRE) on the build machine—**`bin/jdeps`, `bin/jlink`, `bin/jpackage`** must exist.
- **Toolchain resolution** (first match wins): **`JDK_PACKAGE_HOME`**, then **`packaging-jdk.local.xml`** in the repo root (copy from **`packaging-jdk.local.xml.example`**; defines **`jdk.package.home`** — overrides **`JAVA_HOME`** so you can keep distro **`JAVA_HOME`** for your IDE and point packaging at Temurin), then **`JAVA_HOME`**, then **`build/.packaging-jdk/current`** if that path contains **`bin/jdeps`** (unpack Temurin under **`build/.packaging-jdk/`** and `ln -sfn jdk-21.x build/.packaging-jdk/current`; **`ant clean` removes it** with **`./build/`**), then **`java.home`** from the JVM running Ant.

### Fedora / RPM OpenJDK note

Some Linux distro packages **patch `conf/security/java.security`**. `jlink` may fail with:

`Error: …/java.security has been modified`

Use an **unaltered JDK** install for packaging (recommended: **[Eclipse Temurin](https://adoptium.net/) tarball** unpacked locally, then `export JDK_PACKAGE_HOME=/path/to/jdk-21`). The project itself does **not** require JavaFX bundles for `jlink`.

**Ant:** if the resolved packaging JDK path looks like a typical distro **`java-*-openjdk`** layout, **`build.xml`** fails in **`package-deps-properties`** with this hint **before** **`jlink`** runs (avoids the cryptic `java.security has been modified` message deep in the packaging chain).

### Windows `exe`

`ant package-windows-exe` requires running on **Windows** with **WiX** toolset available to `jpackage` (see Oracle/jpackage docs). Prefer **`package-linux-app-image` + zip** when WiX is not installed.

### Git backend

The optional Git sync feature invokes the **`git`** binary on **`PATH`**; it is not bundled.

## Targets (Ant)

Typical ordering:

1. **`ant package-deps-properties`** — verifies `jdeps` / `jlink` / `jpackage` exist.
2. **`ant package-stage`** — builds **`build/classes`**, copies **`data/db`** into **`build/dist/package/prod-db-work/`**, runs **`CreateProdDb`** on that tree (needs maintainer **`src/dist/*.prod`** files; leaves repo-root **`data/db`** untouched), copies Swing-only **`lib/**/*.jar`**, **`data/`** (non-**`db`** tree plus seeded **`data/db`** from **`prod-db-work`**), **`brewday.cfg`**, and **`build/dist/package/stage/brewday.jar`** with **`Main-Class`** and **`Class-Path`** manifest pointing at **`lib/`** jars.
3. **`ant package-jdeps-scan`** — writes **`build/dist/package/jdeps-line.raw.txt`** and merged module sets for **`jlink`** (see **`build/dist/package/jdeps-modules.merge.txt`**).
4. **`ant package-jlink-runtime`** — creates **`build/dist/package/runtime-linux`** or **`build/dist/package/runtime-windows`** (named from the **host OS** running the build). **Re-run release builds separately on Linux and Windows**; do not reuse one platform’s runtime on the other.
5. **`ant package-linux-app-image`** — **Linux / macOS only** → **`build/dist/package/out/linux/Brewday/`** (capital **B** from `--name`). Passes **`jpackage --icon`** with **`data/img/brewday.png`** (PNG required on Linux; launcher / `.desktop` branding).
6. **`ant package-windows-exe`** — **Windows only** → installer/exe under **`build/dist/package/out/windows/`**.

Convenience / legacy:

- **`ant dist`** (default) — runs **`zipdist`** then **`package-linux-app-image`**: staging zip plus Linux/macOS **app-image** with embedded runtime (not runnable on Windows hosts; use **`ant zipdist`** alone there, or **`package-windows-exe`** on Windows + WiX).
- **`ant zipdist`** — zips **`build/dist/package/stage/`** to **`build/dist/brewday_${version}_staging.zip`**. End users still need a **system JDK** if they run `java -jar brewday.jar`; for an embedded runtime use **`package-linux-app-image`** / **`package-windows-exe`**.
- **`ant package-complete`** — prints which final target to run next.

Manual CLI equivalents mirror the **`exec`** steps in **`build.xml`** (see **`package-jdeps-scan`**, **`package-jlink-runtime`**, **`package-linux-app-image`**).

**Compile vs full clean:** `compile` clears only **`build/classes`** (and **`build/test-classes`**), not **`build/dist`**, so multiple packaging targets in one **`ant`** invocation keep prior outputs until you run **`ant clean`** (deletes all of **`./build/`**).

**Layout:** all packaging deliverables and intermediates live under **`build/dist/`**: the staging zip at **`build/dist/brewday_${version}_staging.zip`**, and **`build/dist/package/`** for **`stage/`**, **`prod-db-work/`** (**`CreateProdDb`** — removed by **`ant clean`**), **`runtime-*`**, **`out/`** (jpackage), and jdeps scratch files.

**Linux app-image cwd:** the `jpackage` launcher typically starts with **`user.dir`** under **`…/Brewday/bin/`** while **`brewday.cfg`** and **`data/`** live in **`…/Brewday/lib/app/`**. **`AppContentRoot.install()`** (called from **`SwingApp.main`**) detects **`brewday.cfg`** and sets **`brewday.content.root`** so **`Brewday`**, **`Database`**, and Swing icon file fallbacks resolve paths correctly.

## Outputs

| Artifact | Location |
|---------|----------|
| Staged app (classpath) | **`build/dist/package/stage/`** |
| `jlink` runtime image | **`build/dist/package/runtime-{linux|windows}/`** |
| Linux app-image | **`build/dist/package/out/linux/Brewday/bin/Brewday`** (launcher name may vary slightly by JDK); built with **`--icon`** → **`data/img/brewday.png`** |
| Windows exe (WiX path) | **`build/dist/package/out/windows/`** |
| Staging zip (classpath) | **`build/dist/brewday_${version}_staging.zip`** |

Version strings come from [`src/dist/dist.brewday.cfg`](src/dist/dist.brewday.cfg) (`mclachlan.brewday.version`). **`jpackage`** passes that value verbatim to **`--app-version`** (leading **`v`** is accepted by recent **`jpackage`**; trim in Ant if your tooling rejects it).

## `jpackage` / FlatLAF

Staging injects **`--add-opens`** for common FlatLAF / Swing internals. If **`InaccessibleObjectException`** persists, extend the **`arg`** blocks in **`package-linux-app-image`** / **`package-windows-exe`** in [`build.xml`](../build.xml).

### Linux desktop icon (`--icon`)

**`package-linux-app-image`** passes **`--icon`** pointing at **[`data/img/brewday.png`](../data/img/brewday.png)** (repository path). **`jpackage`** on Linux expects a **PNG** for **`--icon`** (ICO is for Windows). If the file is missing, **`build.xml`** fails before invoking **`jpackage`**.

### VM splash (`-splash`)

**`package-linux-app-image`** and **`package-windows-exe`** pass **`-splash:$$APPDIR/data/img/brewday_splash.bmp`** in Ant ( **`$$`** becomes **`$`** so **`jpackage`** receives **`$APPDIR`**). The BMP must live under **`data/img/`** in the repo so **`package-stage`** copies it into the **`--input`** tree; see Oracle *Support Application Features* (**`$APPDIR`** in **`--java-options`**). HotSpot **`-splash`** image support is platform-dependent; if a given OS ignores BMP, switch to PNG/JPEG and update the path in **`build.xml`**.

## Smoke checks after packaging

- Launch from **`Brewday` app-image** directory; working directory should be the app root (where **`brewday.cfg`** lives next to **`data/`** per [`dist.brewday.cfg`](../src/dist/dist.brewday.cfg)). A brief **VM splash** ( **`brewday_splash.bmp`** ) should appear at process start before the Swing UI.
- Open a recipe and run **Water Builder / LP path** once (covers **Commons Math** + **`java.desktop`**).
- Exercise **Export** / FreeMarker-backed document generation (`./data/templates` on disk).

## Troubleshooting

| Symptom | Likely cause |
|---------|----------------|
| `NoClassDefFoundError` for a Swing dependency | **`package-stage`** classpath mismatch or incomplete **`lib/`** copy next to **`brewday.jar`**. |
| `java.security has been modified` from **`jlink`** | Distro JDK; switch to Temurin (or vanilla tarball) **`JDK_PACKAGE_HOME`**. |
| **`CreateProdDb` Java Result 1** | Missing **`src/dist/*.prod`** seed lists (e.g. **[`src/dist/waterparameters.prod`](src/dist/waterparameters.prod)**). Restore maintainer prod seed inputs or tolerate partial **`data/`** from repo. |
| Headless **`ant test`** failures | Unrelated to packaging; Swing tests need **`xvfb-run`** (see [`doc/bug-backlog.md`](bug-backlog.md)). |
