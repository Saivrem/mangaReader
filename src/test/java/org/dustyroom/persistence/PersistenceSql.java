package org.dustyroom.persistence;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dustyroom.be.persistence.schema.SqlitePragmas;

@RequiredArgsConstructor
enum PersistenceSql {
    SCHEMA_VERSION(SqlitePragmas.READ_VERSION),
    CREATE_FUTURE_DATA("CREATE TABLE future_data(value TEXT)"),
    READ_FUTURE_DATA("SELECT value FROM future_data"),
    INSERT_FUTURE_DATA("INSERT INTO future_data(value) VALUES(?)"),
    DROP_COMPONENTS("DROP TABLE reader_components"),
    DROP_CHAPTERS("DROP TABLE library_chapters"),
    DROP_WORKS("DROP TABLE library_works"),
    UPDATE_COMPONENT("UPDATE reader_components SET version=? WHERE component=?"),
    DELETE_SETTING("DELETE FROM settings WHERE key=?"),
    UPDATE_SETTING("UPDATE settings SET value=? WHERE key=?"),
    BLOCK_BOOK_DELETE("""
            CREATE TRIGGER prevent_delete BEFORE DELETE ON books
            BEGIN SELECT RAISE(ABORT,'test failure'); END"""),
    CREATE_BLOCKED_TITLES("CREATE TABLE blocked_titles(title TEXT PRIMARY KEY)"),
    BLOCK_TITLE("INSERT INTO blocked_titles(title) VALUES(?)"),
    REJECT_BLOCKED_CHAPTER("""
            CREATE TRIGGER prevent_chapter BEFORE INSERT ON library_chapters
            WHEN NEW.title IN (SELECT title FROM blocked_titles)
            BEGIN SELECT RAISE(ABORT,'test failure'); END"""),
    CREATE_ORIGINAL_BOOKS("CREATE TABLE books(original TEXT)"),
    INSERT_ORIGINAL_BOOK("INSERT INTO books(original) VALUES(?)"),
    READ_ORIGINAL_BOOK("SELECT original FROM books"),
    COUNT_TABLES("SELECT count(*) FROM sqlite_master WHERE type='table' AND name=?"),
    SET_BOOK_TIMESTAMP("UPDATE books SET updated_at=? WHERE source_id=?"),
    CREATE_LEGACY_SETTINGS("CREATE TABLE settings(key TEXT PRIMARY KEY,value TEXT NOT NULL)"),
    INSERT_LEGACY_SETTING("INSERT INTO settings(key,value) VALUES(?,?)"),
    CREATE_LEGACY_BOOKS("""
            CREATE TABLE books(
                source_id TEXT PRIMARY KEY, page_id TEXT NOT NULL, page_number INTEGER NOT NULL,
                page_count INTEGER NOT NULL, two_page INTEGER NOT NULL, reading_mode TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )"""),
    INSERT_LEGACY_BOOK("""
            INSERT INTO books(source_id,page_id,page_number,page_count,two_page,reading_mode,updated_at)
            VALUES(?,?,?,?,?,?,?)""");

    @Getter
    private final String sql;
}
