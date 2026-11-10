package org.dustyroom.be.persistence.schema;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RecoverySql {
    public static final String BUSY_TIMEOUT = "PRAGMA busy_timeout=3000";
    public static final String DISABLE_FOREIGN_KEYS = "PRAGMA foreign_keys=OFF";
    public static final String SELECT_OBJECTS = """
                        SELECT type,name FROM sqlite_master
                        WHERE type IN ('view','table') AND name NOT LIKE 'sqlite_%'
                        ORDER BY CASE type WHEN 'view' THEN 0 ELSE 1 END
                        """;
    public static final String RESET_VERSION = "PRAGMA user_version=0";
    public static final String DROP_OBJECT = "DROP %s IF EXISTS \"%s\"";
}
