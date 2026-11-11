package org.dustyroom.be.persistence.schema;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SqlitePragmas {
    public static final String BUSY_TIMEOUT = "PRAGMA busy_timeout=3000";
    public static final String READ_VERSION = "PRAGMA user_version";
    private static final String SET_VERSION = "PRAGMA user_version=%d";

    // SQLite requires an integer literal in a PRAGMA assignment.
    public static String schemaVersion(int version) {
        return SET_VERSION.formatted(version);
    }
}
