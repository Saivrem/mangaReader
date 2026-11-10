package org.dustyroom.be.persistence.schema;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ReaderSql {
    public static final String BUSY_TIMEOUT = "PRAGMA busy_timeout=3000";
    public static final String READ_VERSION = "PRAGMA user_version";
    public static final String ENABLE_WAL = "PRAGMA journal_mode=WAL";
    public static final String CREATE_SETTINGS = "CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)";
    public static final String CREATE_BOOKS = """
                        CREATE TABLE books (
                            source_id TEXT PRIMARY KEY,
                            page_id TEXT NOT NULL,
                            page_number INTEGER NOT NULL CHECK(page_number >= 1),
                            page_count INTEGER NOT NULL CHECK(page_count >= page_number),
                            two_page INTEGER NOT NULL CHECK(two_page IN (0,1)),
                            reading_mode TEXT NOT NULL CHECK(reading_mode IN ('MANGA','COMICS')),
                            updated_at INTEGER NOT NULL
                        )
                        """;
    public static final String VERSION_ONE = "PRAGMA user_version=1";
    public static final String ADD_RECENT_HIDDEN = "ALTER TABLE books ADD COLUMN recent_hidden INTEGER NOT NULL DEFAULT 0 CHECK(recent_hidden IN (0,1))";
    public static final String CREATE_RECENT_INDEX = "CREATE INDEX books_recent ON books(recent_hidden, updated_at DESC)";
    public static final String VERSION_TWO = "PRAGMA user_version=2";
    public static final String SELECT_SETTINGS = "SELECT key,value FROM settings";
    public static final String SAVE_SETTING = "INSERT INTO settings(key,value) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value";
    public static final String SELECT_BOOK = "SELECT * FROM books WHERE source_id=?";
    public static final String SAVE_BOOK = """
                INSERT INTO books(source_id,page_id,page_number,page_count,two_page,reading_mode,updated_at)
                VALUES(?,?,?,?,?,?,?) ON CONFLICT(source_id) DO UPDATE SET
                page_id=excluded.page_id,page_number=excluded.page_number,page_count=excluded.page_count,
                two_page=excluded.two_page,reading_mode=excluded.reading_mode,updated_at=excluded.updated_at
                """;
    public static final String SELECT_RECENTS = "SELECT * FROM books WHERE recent_hidden=0 ORDER BY updated_at DESC, source_id LIMIT ?";
    public static final String SET_RECENT_HIDDEN = "UPDATE books SET recent_hidden=? WHERE source_id=?";
    public static final String CLEAR_CHAPTERS = "DELETE FROM library_chapters";
    public static final String CLEAR_WORKS = "DELETE FROM library_works";
    public static final String CLEAR_SETTINGS = "DELETE FROM settings";
    public static final String CLEAR_BOOKS = "DELETE FROM books";
}
