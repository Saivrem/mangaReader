package org.dustyroom.be.persistence.schema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import lombok.experimental.UtilityClass;

import static org.dustyroom.be.persistence.schema.LibrarySchemaSql.*;

/** Additive library tables have their own version; the reader's schema remains compatible with 0.9. */
@UtilityClass
public class LibrarySchema {
    public static final int VERSION = 1;

    public static int version(Connection connection) throws SQLException {
        if (!tables(connection).contains("reader_components")) {
            return 0;
        }
        try (Statement sql = connection.createStatement();
             ResultSet rows = sql.executeQuery(LIBRARY_VERSION)) {
            return rows.next() ? rows.getInt(1) : 0;
        }
    }

    public static boolean transitionalSchema(Connection connection) throws SQLException {
        // Recognize only the exact short-lived MR-library schema. Never downgrade an unknown future database.
        if (!tables(connection).equals(Set.of("settings", "books", "library_works", "library_chapters"))) {
            return false;
        }
        if (!tableSql(connection, "library_works").equals(WORKS) || !tableSql(connection, "library_chapters").equals(CHAPTERS)) {
            return false;
        }
        return columns(connection, "settings").equals(Set.of("key", "value"))
                && columns(connection, "books").equals(Set.of("source_id", "page_id", "page_number", "page_count", "two_page", "reading_mode", "updated_at", "recent_hidden"));
    }

    public static void create(Connection connection, boolean transitional) throws SQLException {
        try (Statement sql = connection.createStatement()) {
            if (!transitional) {
                sql.execute(WORKS);
                sql.execute(CHAPTERS);
                sql.execute(CREATE_CHAPTER_INDEX);
            }
            sql.execute(CREATE_COMPONENTS);
            sql.execute(INSERT_LIBRARY_VERSION);
        }
    }

    private static Set<String> tables(Connection connection) throws SQLException {
        Set<String> names = new HashSet<>();
        try (Statement sql = connection.createStatement(); ResultSet rows = sql.executeQuery(LIST_TABLES)) {
            while (rows.next()) {
                names.add(rows.getString(1));
            }
        }
        return names;
    }

    private static String tableSql(Connection connection, String table) throws SQLException {
        try (PreparedStatement sql = connection.prepareStatement(TABLE_SQL)) {
            sql.setString(1, table);
            try (ResultSet rows = sql.executeQuery()) {
                return rows.next() ? rows.getString(1) : "";
            }
        }
    }

    private static Set<String> columns(Connection connection, String table) throws SQLException {
        Set<String> names = new HashSet<>();
        try (PreparedStatement sql = connection.prepareStatement(TABLE_COLUMNS)) {
            sql.setString(1, table);
            try (ResultSet rows = sql.executeQuery()) {
                while (rows.next()) {
                    names.add(rows.getString("name"));
                }
            }
        }
        return names;
    }
}
