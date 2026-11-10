package org.dustyroom.be.persistence;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.dustyroom.be.persistence.schema.RecoverySql;

/** Explicit user-confirmed reset only. Never invoked by initialization or migration. */
@UtilityClass
final class DatabaseRecovery {

    static void reset(Path database) {
        Path path = database.toAbsolutePath().normalize();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path)) {
            try (Statement sql = connection.createStatement()) {
                sql.execute(RecoverySql.BUSY_TIMEOUT);
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
                sql.execute(RecoverySql.RESET_VERSION);
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
