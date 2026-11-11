package org.dustyroom.be.persistence.schema;

import lombok.experimental.UtilityClass;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@UtilityClass
public class ReaderSchema {
    public static final int VERSION = 2;

    public static void migrate(Connection connection, int version, int libraryVersion, boolean transitional) throws SQLException {
        if (version == VERSION && libraryVersion == LibrarySchema.VERSION) {
            return;
        }
        connection.setAutoCommit(false);
        try (Statement sql = connection.createStatement()) {
            // Add future migrations in version order; never recreate existing tables on startup.
            if (version < 1) {
                sql.execute(ReaderSql.CREATE_SETTINGS);
                sql.execute(ReaderSql.CREATE_BOOKS);
                sql.execute(SqlitePragmas.schemaVersion(1));
            }
            if (version < 2) {
                sql.execute(ReaderSql.ADD_RECENT_HIDDEN);
                sql.execute(ReaderSql.CREATE_RECENT_INDEX);
                sql.execute(SqlitePragmas.schemaVersion(2));
            }
            if (libraryVersion < 1) {
                LibrarySchema.create(connection, transitional);
            }
            if (transitional) {
                sql.execute(SqlitePragmas.schemaVersion(2));
            }
            try (ResultSet result = sql.executeQuery(SqlitePragmas.READ_VERSION)) {
                if (!result.next() || result.getInt(1) != VERSION) {
                    throw new SQLException("Missing database migration; refusing a partial upgrade");
                }
            }
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(true);
        }
    }

}
