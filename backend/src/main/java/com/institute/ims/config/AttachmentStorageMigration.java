package com.institute.ims.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AttachmentStorageMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public AttachmentStorageMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        migrateLargeObjectColumn("assignment_attachments");
        migrateLargeObjectColumn("submission_attachments");
        backfillStudyMaterialUploadTimes();
    }

    private void migrateLargeObjectColumn(String tableName) {
        Integer oidColumnCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = current_schema()
                          AND table_name = ?
                          AND column_name = 'data'
                          AND udt_name = 'oid'
                        """,
                Integer.class,
                tableName);

        if (oidColumnCount != null && oidColumnCount > 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE " + tableName
                            + " ALTER COLUMN data TYPE bytea USING lo_get(data)");
        }
    }

    private void backfillStudyMaterialUploadTimes() {
        Integer uploadTimeColumnCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = current_schema()
                          AND table_name = 'study_materials'
                          AND column_name = 'uploaded_at'
                        """,
                Integer.class);
        if (uploadTimeColumnCount != null && uploadTimeColumnCount > 0) {
            jdbcTemplate.update(
                    "UPDATE study_materials SET uploaded_at = CURRENT_TIMESTAMP WHERE uploaded_at IS NULL");
        }
    }
}
