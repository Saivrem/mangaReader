# Project guidance

## Runtime and verification

- This is a Java 17 Swing desktop application built with the checked-in Gradle wrapper.
- Run the Gradle wrapper itself with JDK 17. The current Gradle 8.4 wrapper is not compatible with a Java 25 runtime, and the Java toolchain setting does not change the JVM that starts Gradle.
- Before finishing code changes, run `./gradlew clean test fatJar`.
- For UI changes, report separately whether the behavior was manually verified in the desktop application.

## Architecture

- `src/main/java/org/dustyroom/be/iterators` owns image, directory, and ZIP navigation.
- Iterators expose stable `PageRef` values and load images separately. Keep navigation/history lightweight; do not retain `BufferedImage` instances there.
- Iterator initialization and volume switching must be transactional: a failed candidate source must leave the active source usable.
- `src/main/java/org/dustyroom/ui/loading` owns background source creation, decoding, cache, prefetch, request ordering, and source lifecycle.
- `src/main/java/org/dustyroom/ui/navigation` decides page and spread navigation without rendering Swing components.
- `src/main/java/org/dustyroom/ui/rendering` composes images for display.
- `ViewerController` is an EDT-facing coordinator. Keep filesystem traversal, archive access, image decoding, and spread composition out of it.
- Keep all operations on a mutable iterator or ZIP source on the same serial worker, including deterministic close after in-flight work.

## Conventions

- Keep filesystem and image-decoding work off the Swing event dispatch thread.
- Preserve generation/request checks when changing asynchronous loading so stale frames, errors, and prefetch results cannot update the UI.
- Keep decoded images in the bounded LRU cache only. Cache eviction removes references but must not call `BufferedImage.flush()`, because the UI may still display the image.
- Close streams and archives deterministically.
- Preserve natural file ordering and keep image formats separate from archive formats.
- Add tests for iterator boundaries and navigation behavior when changing those areas.
- Add headless unit tests for asynchronous ordering, stale-result suppression, cache eviction, source replacement, and shutdown races when changing the loading pipeline.
- Preserve unrelated user changes and do not edit generated files under `build/`, `.gradle/`, `out/`, or `.idea/`.

## Versioning

- Treat substantial user-visible features, architectural changes, and other release-level work as versioned changes.
- For such changes, increment the project version in `build.gradle` and the matching About-dialog version in `src/main/java/org/dustyroom/ui/utils/DialogUtils.java`. Keep both values identical.
- When the version changes, update versioned artifact names and commands in `README.md` as well.
- Do not bump the version for documentation-only changes, tests, or small internal fixes unless the user explicitly requests a release.
