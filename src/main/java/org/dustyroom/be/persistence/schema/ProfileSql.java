package org.dustyroom.be.persistence.schema;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ProfileSql {
    public static final String CHECK_DATABASE = "PRAGMA quick_check";
    public static final String CHECKPOINT = "PRAGMA wal_checkpoint(TRUNCATE)";
    public static final String CLOSE_JOURNAL = "PRAGMA journal_mode=DELETE";
}
