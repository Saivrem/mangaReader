package org.dustyroom.be.persistence;

import org.dustyroom.be.utils.SqliteUtils;
import org.dustyroom.be.persistence.schema.SqlitePragmas;
import lombok.experimental.UtilityClass;
import org.dustyroom.be.persistence.schema.RecoverySql;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Explicit user-confirmed reset only. Never invoked by initialization or migration.
 */
@UtilityClass
final class DatabaseRecovery {

    static void reset(Path database) {
        Path path = database.toAbsolutePath().normalize();
        try (Connection connection = SqliteUtils.connect(path)) {
            try (Statement sql = connection.createStatement()) {
                sql.execute(SqlitePragmas.BUSY_TIMEOUT);
                sql.execute(RecoverySql.DISABLE_FOREIGN_KEYS);
            }
            connection.setAutoCommit(false);
            try (Statement sql = connection.createStatement()) {
                List<String> drops = new ArrayList<>();
                try (ResultSet rows = sql.executeQuery(RecoverySql.SELECT_OBJECTS)) {
                    while (rows.next()) {
                        String kind = rows.getString(1).equals("view") ? "VIEW" : "TABLE";
                        String name = rows.getString(2).replace("\"", "\"\"");
                        drops.add(RecoverySql.DROP_OBJECT.formatted(kind, name));
                    }
                }
                for (String drop : drops) {
                    sql.execute(drop);
                }
                sql.execute(SqlitePragmas.schemaVersion(0));
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot safely reset reader data. The reset failed; the existing database was kept.", exception);
        }
    }
}
