# Project guidance

## Runtime and verification

- This is a Java 17 Swing desktop application built with the checked-in Gradle wrapper.
- Before finishing code changes, run `./gradlew clean test fatJar`.
- For UI changes, report separately whether the behavior was manually verified in the desktop application.

## Architecture

- `src/main/java/org/dustyroom/be/iterators` owns image, directory, and ZIP navigation.
- `src/main/java/org/dustyroom/ui/navigation` decides page and spread navigation without rendering Swing components.
- `src/main/java/org/dustyroom/ui/rendering` composes images for display.
- `ViewerController` coordinates UI state; keep filesystem traversal and image decoding out of it.

## Conventions

- Keep filesystem and image-decoding work off the Swing event dispatch thread.
- Close streams and archives deterministically.
- Preserve natural file ordering and keep image formats separate from archive formats.
- Add tests for iterator boundaries and navigation behavior when changing those areas.
- Preserve unrelated user changes and do not edit generated files under `build/`, `.gradle/`, `out/`, or `.idea/`.
