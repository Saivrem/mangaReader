package org.dustyroom.be.persistence.schema;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.experimental.UtilityClass;

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
                sql.execute(ReaderSql.VERSION_ONE);
            }
            if (version < 2) {
                sql.execute(ReaderSql.ADD_RECENT_HIDDEN);
                sql.execute(ReaderSql.CREATE_RECENT_INDEX);
                sql.execute(ReaderSql.VERSION_TWO);
            }
            if (libraryVersion < 1) {
                LibrarySchema.create(connection, transitional);
            }
            if (transitional) {
                sql.execute(ReaderSql.VERSION_TWO);
            }
            try (ResultSet result = sql.executeQuery(ReaderSql.READ_VERSION)) {
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
