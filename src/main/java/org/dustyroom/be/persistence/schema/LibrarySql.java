package org.dustyroom.be.persistence.schema;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LibrarySql {
    public static final String SELECT_WORKS = "SELECT * FROM library_works WHERE active=1 ORDER BY added_at DESC,id";
    public static final String SELECT_CHAPTERS = """
                SELECT c.*,b.page_number,b.page_count,b.updated_at FROM library_chapters c
                LEFT JOIN books b ON b.source_id=c.source_path WHERE c.work_id=?
                """;
    public static final String IMPORT_WORK = """
                        INSERT INTO library_works(id,root,title,added_at,active) VALUES(?,?,?,?,1)
                        ON CONFLICT(root) DO UPDATE SET active=1
                        """;
    public static final String FIND_WORK = "SELECT id FROM library_works WHERE root=?";
    public static final String IMPORT_CHAPTER = """
                        INSERT INTO library_chapters(id,work_id,source_path,title,directory,finished)
                        VALUES(?,?,?,?,?,0) ON CONFLICT(source_path) DO NOTHING
                        """;
    public static final String REMOVE_WORK = "UPDATE library_works SET active=0 WHERE id=?";
    public static final String RENAME_WORK = "UPDATE library_works SET title=? WHERE id=?";
    public static final String SET_CHAPTER_FINISHED = "UPDATE library_chapters SET finished=? WHERE id=?";
}
