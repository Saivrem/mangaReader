package org.dustyroom.be.persistence.schema;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LibrarySchemaSql {
    public static final String WORKS = "CREATE TABLE library_works (id TEXT PRIMARY KEY, root TEXT NOT NULL UNIQUE, title TEXT NOT NULL, added_at INTEGER NOT NULL, active INTEGER NOT NULL CHECK(active IN (0,1)))";
    public static final String CHAPTERS = "CREATE TABLE library_chapters (id TEXT PRIMARY KEY, work_id TEXT NOT NULL REFERENCES library_works(id), source_path TEXT NOT NULL UNIQUE, title TEXT NOT NULL, directory INTEGER NOT NULL CHECK(directory IN (0,1)), finished INTEGER NOT NULL CHECK(finished IN (0,1)))";
    public static final String LIBRARY_VERSION = "SELECT version FROM reader_components WHERE component='library'";
    public static final String CREATE_CHAPTER_INDEX = "CREATE INDEX library_chapters_work ON library_chapters(work_id)";
    public static final String CREATE_COMPONENTS = "CREATE TABLE IF NOT EXISTS reader_components(component TEXT PRIMARY KEY, version INTEGER NOT NULL)";
    public static final String INSERT_LIBRARY_VERSION = "INSERT INTO reader_components(component,version) VALUES('library',1)";
    public static final String LIST_TABLES = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'";
    public static final String TABLE_SQL = "SELECT sql FROM sqlite_master WHERE type='table' AND name=?";
    public static final String TABLE_COLUMNS = "SELECT name FROM pragma_table_info(?)";
}
