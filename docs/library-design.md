# Library proposal

Status: design for review, not an implemented library. Application version remains 0.9.0.

## Current foundation: Recently read

The home screen and File → Open Recent share the latest 12 visible books/series.
ZIPs are grouped by parent directory by default (optional, persisted preference).
Grouping considers the latest 1,000 visible states; each card resumes the last-read
chapter and exposes up to 20 recent chapters. Per-chapter state remains independent.
A book means a ZIP archive or a directory of images; opening a single image belongs
to that directory's reading state. Opening from history restores its saved page,
spread mode and reading direction. File → Home returns to the shelf.

SQLite schema 2 adds `books.recent_hidden`. Removing a recent item only sets this
flag. Progress updates do not unhide it; explicitly reopening the source does.
Existing books enter history on migration, sorted by `updated_at`, which records
reading-state changes rather than strictly the time of opening. The complete state
history is retained even when only 12 entries are shown.

Covers use the first page, decoded on the serial background worker with low priority,
one source per task. At most 12 reduced covers are retained by the shelf. They are
not persisted. Unreadable covers get a placeholder; missing/unreadable paths are
marked unavailable. Refreshing Home or Open Recent rechecks availability. We do not
claim to distinguish a moved file from an offline disk. Paths appear in tooltips to
disambiguate equal filenames. History does not scan the filesystem for missing books.

## Product boundaries

Recent history answers “what was I reading?” A library answers “what do I own and
want to read?” Library membership must be explicit and independent of recent history.
Removing a recent item must never remove a library entry, bookmarks, reading state,
or files. Removing a library entry must likewise never delete files by default.

Proposed first library iteration:

- Add a book or a library folder explicitly; never scan HOME automatically.
- Grid/list views with covers, title, progress and availability; search by title/path.
- Sort by title, last read and date added; filters for unread, in progress, finished,
  and unavailable. “Finished” should be an explicit status, not inferred from the
  current page: the reader's page navigation wraps around.
- Resume, start from the beginning, locate a moved source and remove from library.
- A refresh action with progress/cancel; filesystem watchers can follow later.

Out of scope for the first iteration: downloads, online metadata, recursive scans
without confirmation, series inference from filenames, cloud sync and file deletion.
Bookmarks remain independent optional work: multiple deliberate positions, not the
single automatic resume position.

## Identity and proposed schema

Current `source_id` is an absolute normalized path; this is sufficient for recents
but does not survive moves. Do not silently rewrite it based on filename matches.

Introduce in a future versioned migration:

- `library_books`: stable generated ID, display title, date added, explicit reading
  status and optional cover override.
- `book_locations`: stable book ID, normalized source path, source type, last checked
  time and availability. Initially one active location per book.
- `reading_state`: stable book ID, stable page identifier plus fallback index, page
  count, spread mode and direction; imported from the existing `books` table.
- `series`: stable series ID, display title, explicit grouping source (initially a
  parent folder). Import existing folder groups only when the user adds them to
  the library; toggling recent grouping must not rewrite library membership.
- `series_items`: series ID, book ID and user-adjustable chapter order. Sort with
  the existing natural filename comparator, not recent-read order. Keep resume
  chapter on the series separate from each chapter's saved position.
- `library_roots`: explicitly added directories and their scan preferences.
- `recent_history`: stable book ID, last-read timestamp, hidden flag; independent of
  library membership. Retain legacy book state until migration validation succeeds.

“Locate…” lets the user explicitly attach a new path to an existing book. Validate
source type and page inventory, show mismatches, and ask before reattaching a different
book. A ZIP content fingerprint can assist later but is not a prerequisite; avoid
hashing entire archives on every launch. Do not merge equal names automatically.

## Scanning and caches

Use cancellable background tasks with bounded work queues and batched UI updates.
Interactive page navigation outranks metadata scans and cover generation. Close
archive handles per source and keep no unbounded decoded-image cache. Do not follow
symlink loops. Scan only supported sources; define a directory containing images as
one book, not one library item per page. Nested folders and archives need explicit,
documented recursion policy before enabling recursive scanning.

Covers are disposable cached derivatives keyed by book ID, source fingerprint and
renderer version. Keep them separate from SQLite state and from user books. Specify
an on-disk byte budget and LRU eviction before introducing persistent caching.
Unavailable sources retain their metadata, cover and position.

## Compatibility, deletion and testing

Schema versions remain independent of app versions. Before any existing database
migration, create a consistent SQLite backup; migrate transactionally, preserve
unknown/newer databases, and test rollback. Never recreate a broken DB silently.

Reset Application explicitly clears app-owned state and future cover caches after
confirmation. Uninstall removes application binaries and OS integration; user books
are never owned by the installer, and per-user reader data remains for reinstalls.

Acceptance cases: migrate schema 1 → 2 → library schema preserving hidden history and
reading modes; equal filenames at different paths; Unicode; offline/remounted drive;
explicit move/relink; corrupt ZIP; cancellation; no I/O on EDT; bounded caches; deletion
never touches books; fresh install, upgrade and uninstall on each supported OS.

## Suggested delivery order

1. Ship recents and validate the shared shelf/menu behavior.
2. Review this design: folder recursion, explicit completion status, and relink UX.
3. Add stable IDs and manual library membership with migration tests.
4. Add search/filter/sort and bounded cover cache.
5. Add cancellable folder import, then optional filesystem watching.

## Series-first library UX

The confirmed user layout is one directory per work, with one ZIP per chapter.
Show one library card per work, with a Continue button for its last-read chapter.
Opening the card reveals all imported chapters in natural order, individual progress,
and explicit unread/in-progress/finished status. A recent chapter does not occupy
another top-level card. Folder names provide initial titles; users can rename the
library title without renaming files. A folder with image pages remains a single
book, while a folder containing chapter ZIPs is a series. If both occur, present the
import preview for explicit selection rather than silently mixing them.

First library milestone should be manual folder import and this series detail view,
then search/filter and relink. Recents are already useful independently, so full
library implementation remains a reviewed follow-up rather than a prerequisite for
these reader fixes.
