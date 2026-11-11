package org.dustyroom.persistence;

import org.dustyroom.be.utils.SqliteUtils;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.dustyroom.persistence.PersistenceSql.*;
import static org.junit.jupiter.api.Assertions.*;

class SqliteQueryTest extends BasePersistenceTest {
    @Test
    void incompleteBatchRowCannotReuseValuesFromPreviousRow() throws SQLException {
        execute(CREATE_FUTURE_DATA);
        String value = "Reader's 夜中";

        try (Connection connection = SqliteUtils.connect(database);
             PreparedStatement insert = connection.prepareStatement(INSERT_FUTURE_DATA.getSql())) {
            SqliteUtils.addBatch(insert, value);
            assertThrows(SQLException.class, () -> SqliteUtils.addBatch(insert));
            insert.executeBatch();
        }

        assertEquals(value, scalar(READ_FUTURE_DATA));
    }
}
