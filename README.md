# Manga Reader

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
build/libs/mangaReader-0.8.0.jar
```

## Run

Open the application without an initial file:

```bash
java -jar build/libs/mangaReader-0.8.0.jar
```

Open an image, a directory of images, or a ZIP archive immediately:

```bash
java -jar build/libs/mangaReader-0.8.0.jar /path/to/page.jpg
java -jar build/libs/mangaReader-0.8.0.jar /path/to/chapter
java -jar build/libs/mangaReader-0.8.0.jar /path/to/volume.zip
```

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

## Appearance and platform behavior

- Options → Theme selects Manga Reader Ayu Dark (default) or Ayu Light.
- Both application themes use the [Ayu palette](https://github.com/ayu-theme/ayu-colors), implemented with FlatLaf.
- macOS uses the global screen menu and Command shortcuts.
- Windows and Linux use a menu inside the window, Control shortcuts, and native window decorations. Windows retains its system window controls and Alt+F4 behavior.
- The native title bar and macOS global menu follow the OS appearance; the theme selector colors the application content.
- Theme selection applies immediately for the current session.

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
| `Q`, `Escape`, `Cmd/Ctrl+Q` | Exit |

## Project structure

- `src/main/java/org/dustyroom/be` — file discovery, ordering, models, and image iterators
- `src/main/java/org/dustyroom/ui` — Swing window, actions, components, navigation, and rendering
- `src/test/java` — automated tests

The main entry point is `org.dustyroom.Main`.

## Known limitations

- The file chooser accepts files only; directories can be passed on the command line.
- Image rotation is not supported.

## Development notes

Repository-specific guidance for coding agents and contributors is available in [`AGENTS.md`](AGENTS.md).
