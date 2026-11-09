# Yonaka Reader ・ 夜中

A lightweight desktop manga and image viewer built with Java Swing.

## Requirements

- JDK 17

The Gradle wrapper is included, and the build uses a Java 17 toolchain. A separate Gradle installation is not required.

## Build and test

Run the full verification and build an executable fat JAR:

```bash
./gradlew clean test fatJar
```

The resulting application is written to:

```text
build/libs/yonaka-reader-0.9.0.jar
```

## Run

Open the application without an initial file:

```bash
java -jar build/libs/yonaka-reader-0.9.0.jar
```

Open an image, a directory of images, or a ZIP archive immediately:

```bash
java -jar build/libs/yonaka-reader-0.9.0.jar /path/to/page.jpg
java -jar build/libs/yonaka-reader-0.9.0.jar /path/to/chapter
java -jar build/libs/yonaka-reader-0.9.0.jar /path/to/volume.zip
```

## Recently read

The home screen shows up to 12 recently read books with a first-page cover and saved
page progress. The same list is available under **File → Open Recent**. Use
**File → Home** to return to the shelf while a book is open.

Click a book to resume its saved page, spread mode and reading direction. Images
opened individually share the history entry of their containing directory.
**Remove from recent** hides an entry without deleting files or saved progress;
opening that book again adds it back. Missing or inaccessible sources remain visible
as unavailable. Reopen Home or Open Recent to refresh their availability after
reconnecting a drive. A moved book must currently be opened from its new location;
automatic move detection and transferring progress to a new path are future library work.

Covers are loaded in the background and kept only in bounded memory. An unreadable
cover uses a placeholder; opening the source reports any actual reading error.
Existing reading history is preserved by a backed-up, transactional schema migration.
See [the library proposal](docs/library-design.md) for the next-stage design.

## Supported content

- Images: `jpg`, `jpeg`, `png`, and `gif`
- Archives: `zip`
- Natural file ordering, for example `page2` before `page10`
- Nested image paths inside ZIP archives
- Single-page and two-page spreads
- Left-to-right comics and right-to-left manga reading modes
- Fit-to-screen, fit-width, fit-height, zoom, scrolling, and fullscreen modes
- Background image decoding with stale-request protection
- Bounded image caching and next-page prefetch

GIF files are currently displayed as static images.

## Reader interface

- A welcome screen provides a single Open action and a supported-format hint.
- The toolbar shows the displayed page or spread range and total page count. Click it or press G to jump to a page.
- Vector navigation icons replace font-dependent symbols and emoji.
- Scale, two-page spread, reading direction and fullscreen states stay synchronized between toolbar, menus and shortcuts.
- Reading controls become available after a book opens. First/last commands are disabled at their respective boundaries; next/previous retain wrap-around navigation.
- Escape leaves fullscreen without closing the application; Q and Cmd/Ctrl+Q still quit.

## Appearance and platform behavior

- Options → Theme selects Yonaka Reader Ayu Dark (default) or Ayu Light.
- Both application themes use the [Ayu palette](https://github.com/ayu-theme/ayu-colors), implemented with FlatLaf.
- macOS uses the global screen menu and Command shortcuts.
- Windows and Linux use a menu inside the window, Control shortcuts, and native window decorations. Windows retains its system window controls and Alt+F4 behavior.
- The native title bar and macOS global menu follow the OS appearance; the theme selector colors the application content.
- Theme and scale preferences persist between launches in the local SQLite database.

## Drag and drop and saved state

Drop one image, ZIP archive, or image folder anywhere in the application content.
The Open dialog also accepts folders. Dropping multiple objects is rejected with an
explanation; unsupported or corrupt sources leave the current book visible.

Open starts in the last successfully opened directory, including after a restart.
A deleted or unreadable directory falls back to the home folder. Cancelled dialogs
and failed opens do not change the saved directory; application reset clears it.

A local SQLite database stores theme, scale and last-directory preferences, plus the last displayed
page, page count, reading direction and spread mode for each book. A book is identified
by the absolute normalized path of its folder or ZIP archive. Reopening it resumes
reading; explicitly opening an image always selects that image. Page names are used
before numeric positions, so inserting earlier pages does not shift the bookmark.
If the saved page was removed, its old position is clamped to the remaining pages.
Moving or renaming a book creates a new history entry.

Storage locations:

- macOS: `~/Library/Application Support/MangaReader/reader.sqlite3`
- Windows: `%APPDATA%/MangaReader/reader.sqlite3` (fallback: `~/AppData/Roaming/MangaReader/reader.sqlite3`)
- Linux: `$XDG_DATA_HOME/manga-reader/reader.sqlite3` (fallback: `~/.local/share/manga-reader/reader.sqlite3`)

For portable storage, override the directory before `-jar`:

```bash
java -Dmangareader.dataDir=./reader-data -jar build/libs/yonaka-reader-0.9.0.jar
```

### Reset and compatibility

Use **Options → Reset Application…** to clear settings and reading history. The
confirmation defaults to Cancel. After a successful reset the app closes; the next
launch uses defaults. Image files, folders and ZIP archives are never deleted.
Reset is transactional: if clearing either table fails, both retain their data.
Queued saves are drained before reset, and late loading callbacks cannot repopulate it.
This is an application reset, not secure erasure of SQLite pages or backup files.

The data directory and database filename do not include the application version.
Application releases such as 0.8 and 0.9 share the same storage location. The database
schema version is independent: an unchanged schema is opened without running DDL.
Future schema upgrades must use ordered transactional migrations, with a consistent
SQLite backup taken before upgrading an existing versioned database. Backup failure
blocks the upgrade. A newer, unsupported schema is rejected without changing the
file; the app can still read books without saving progress. Migration failures never
trigger an automatic wipe or database recreation.

The database uses schema version 2 and WAL journaling; SQLite may create adjacent
`-wal` and `-shm` files. Settings and progress are written on the background worker,
and queued saves finish before normal application exit. A database failure is reported
without preventing reading. No book images or archive contents are stored in the database.

## Keyboard shortcuts

| Key | Action |
| --- | --- |
| `O`, `Cmd/Ctrl+O` | Open a file |
| `Right`, `Page Down` | Next page |
| `Left`, `Page Up` | Previous page |
| `Home`, `End` | First or last page |
| `Up`, `Down` | Previous or next volume |
| `H`, `W`, `S` | Fit height, width, or screen |
| `+`, `-` | Zoom in or out |
| `P` | Toggle two-page mode |
| `M`, `C` | Manga or comics reading mode |
| `F` (also `F11` on Windows/Linux) | Toggle fullscreen |
| `Escape` | Leave fullscreen |
| `G` | Go to page |
| `Q`, `Cmd/Ctrl+Q` | Exit |

## Project structure

- `src/main/java/org/dustyroom/be` — file discovery, ordering, models, and image iterators
- `src/main/java/org/dustyroom/ui` — Swing window, actions, components, navigation, and rendering
- `src/test/java` — automated tests

The main entry point is `org.dustyroom.Main`.

## Known limitations

- Image rotation is not supported.

## Development notes

Repository-specific guidance for coding agents and contributors is available in [`AGENTS.md`](AGENTS.md).

### Page thumbnails

Click the centered handle on the sidebar edge, or use **View → Thumbnails** / **T**,
to show or hide the left page panel. The handle remains available when collapsed; its visibility
is saved between launches. Click a thumbnail to open that page, or focus the list,
select a page with the arrow keys and press Enter. The current page or spread is
highlighted and follows reading progress. The panel is shown once a book is open.

Thumbnails load on the background image worker below reading priority, visible pages
first. A sliding in-memory cache keeps the visible range plus 10 pages before and
10 after it (usually about 25–30 previews, 144 × 192 logical pixels each, backed by up to 432 × 576 pixels for HiDPI screens). Scrolling
evicts previews outside this window and loads the newly needed pages one at a time.
Changing books clears the preview cache. Unreadable previews retain their page number
so the remaining pages can still be browsed.

### Continuous integration

GitHub Actions runs tests, builds the runnable JAR and checks packaged SQLite on
Linux, macOS and Windows using Java 17 for pushes and pull requests. Test reports
and successful JAR builds are available as workflow artifacts for 14 days.
The packaged SQLite check can also be run locally:

```shell
./gradlew clean test fatJar packagedStoreSmoke
```

These checks cover headless behavior; native menus, drag-and-drop and display
scaling still need manual desktop verification on each platform.

### Standalone macOS application

On macOS with JDK 17 and Python 3, run:

```shell
./gradlew clean test packageDmg
```

The DMG is written to `build/macos/dmg/`. Open it, drag **Yonaka Reader.app**
to **Applications**, then eject the disk image. Java is bundled inside the app;
no system Java, installer, administrator service or background agent is needed.
The package matches the architecture of the JDK used to build it. CI currently
builds Apple Silicon; use an Intel JDK on an Intel Mac for an Intel package.

Finder's **Open With → Yonaka Reader** supports `.jpg`, `.jpeg`, `.png`, `.gif`
and `.zip`. Registration does not force a new default application. Opening from
Finder works both at startup and while the reader is already running. If multiple
files are selected, the first is opened because the reader displays one book at a time.
Command-line paths containing spaces must be quoted.

To uninstall, quit Yonaka Reader and move the app from Applications to Trash.
The bundled Java is removed with it. Reading history and settings remain in
`~/Library/Application Support/MangaReader` for future reinstalls. To clear them
as well, use **Options → Reset Application…** before deleting the app. Books are
never deleted by reset or uninstall.

These development packages are ad-hoc signed, not Developer ID signed or notarized;
Gatekeeper may block downloaded builds. Public distribution signing/notarization is
a separate release step. `verifyMacApp` validates the generated file associations,
checks the bundle signature, and tests SQLite using the embedded Java runtime.
Finder integration and drag-to-Applications installation still require a manual check.

The displayed application version comes from the Gradle release version. macOS
`CFBundleVersion` is a separate build identifier with the major component offset
by one, because jpackage requires it to be positive even for a 0.x release.

Branding uses **Yonaka Reader ・ 夜中**. The existing `org.dustyroom.mangareader`
bundle identifier and storage paths remain stable so renaming
the app preserves reading history and preferences. The macOS icon is generated from
`src/main/resources/org/dustyroom/ui/branding/yonaka.png` at standard and Retina sizes.
The original crescent icon is preserved alongside it as `yonaka-original.png`.

### Windows packages

Build on Windows x64 with JDK 17. MSI packaging also requires **WiX Toolset 3.x**
(`candle.exe` and `light.exe` on `PATH`; WiX 4/5 alone are not compatible with this
JDK 17 packaging setup).

```powershell
.\gradlew.bat clean test packageMsi packageWindowsZip
```

Outputs are under `build/desktop-packages/windows/msi/` and
`build/desktop-packages/windows/portable/`. The MSI installs for the current user,
adds Start Menu/desktop shortcuts and file associations, and can be removed through
Windows Settings → Apps. A stable upgrade UUID identifies future MSI upgrades.
The portable ZIP contains `Yonaka Reader.exe` and its Java runtime; extract the
whole directory before launching. Portable builds do not register file associations.
Deleting a portable directory removes the app. Both forms preserve user settings
and reading history; use Reset Application to clear them explicitly.

The binaries are currently unsigned. SmartScreen may warn about an unknown publisher;
Smart App Control or managed-device policies can block unsigned software. There is
no Apple-style notarization step, but trusted signing remains a separate release task.

### Ubuntu packages

Build on Ubuntu 22.04 or newer with JDK 17 and `fakeroot` (`sudo apt install fakeroot`).
CI targets Ubuntu 22.04 x64 to avoid unnecessarily raising the system-library baseline.

```shell
./gradlew clean test packageDeb packageLinuxTar
sudo apt install ./build/desktop-packages/linux/deb/*.deb
```

The DEB installs the application with a desktop launcher, icon and file associations.
Remove it with `sudo apt remove yonaka-reader`. Installing this local DEB does not
require a developer signing certificate; a future APT repository would need its own
repository signing setup. Configuration and history remain in the user's data directory.

For a portable copy, extract the archive from `build/desktop-packages/linux/portable/`
and launch `Yonaka Reader/bin/Yonaka Reader`. Keep the entire extracted directory;
Java is included, but graphical system libraries and a desktop session are still needed.

All package tasks appear under Gradle's **distribution** group and must run on the
target OS. CI tests the embedded runtime before packaging, then extracts each MSI/DEB
and repeats the SQLite smoke test with the extracted runtime. On disposable GitHub-hosted
runners it also installs and uninstalls MSI/DEB, checks that binaries are removed and
that sentinel user data/books remain. Windows checks the uninstall registration too.
The lifecycle script refuses to run on developer workstations. Installer UI and desktop
integration still require manual platform checks. No workflow publishes a release.
