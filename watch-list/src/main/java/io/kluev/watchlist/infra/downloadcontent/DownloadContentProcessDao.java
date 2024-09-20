package io.kluev.watchlist.infra.downloadcontent;

import io.kluev.watchlist.app.downloadcontent.ContentItemIdentity;
import io.kluev.watchlist.app.downloadcontent.DownloadContentProcess;
import io.kluev.watchlist.app.downloadcontent.DownloadContentProcessStatus;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
public class DownloadContentProcessDao {

    public static final String INSERT_PROCESS_SQL =
    """
        INSERT INTO download_content_process (status, content_item_identity, torr_file_path)
        VALUES (?, ?, ?)
    """;

    public static final String UPDATE_PROCESS_SQL =
    """
    UPDATE download_content_process
    SET
        status=:status,
        torr_info_hash=:torrInfoHash,
        content_path=:contentPath,
        updated_at=CURRENT_TIMESTAMP,
        context=:context
    WHERE id=:id;
    """;

    public static final String SELECT_NON_FINISHED_PROCESSES_SQL =
    """
        SELECT id, status, run_iteration, content_item_identity, torr_file_path, torr_info_hash, content_path, created_at, updated_at, next_run_after, context
        FROM download_content_process
        WHERE status not in (:final_statuses)
        LIMIT :limit
    """;

    public final JdbcClient jdbcClient;

    @Transactional
    public boolean save(@NonNull DownloadContentProcess task) {
        val rec = toDbRecord(task);
        val isNew = rec.id() == null;
        if (isNew) {
            return jdbcClient
                    .sql(INSERT_PROCESS_SQL)
                    .params(rec.status(), rec.contentItemIdentity(), rec.torrFilePath())
                    .update() > 0;
        } else {
            return jdbcClient
                    .sql(UPDATE_PROCESS_SQL)
                    .param("id", rec.id())
                    .param("status", rec.status())
                    .param("torrInfoHash", rec.torrInfoHash())
                    .param("contentPath", rec.contentPath())
                    .param("context", rec.context())
                    .update() > 0;
        }
    }

    @Transactional(readOnly = true)
    public List<DownloadContentProcess> getActive() {
        return jdbcClient
                .sql(SELECT_NON_FINISHED_PROCESSES_SQL)
                .param("final_statuses", DownloadContentProcessStatus.FINAL_STATUSES.stream().map(Enum::name).toList())
                .param("limit", 10)
                .query(DownloadContentTaskDbRecordRowMapper.INSTANCE)
                .stream()
                .map(this::toEntity)
                .toList();
    }

    private DownloadContentProcessDbRecord toDbRecord(@NonNull DownloadContentProcess task) {
        return new DownloadContentProcessDbRecord(
                task.getId(),
                task.getStatus().name(),
                task.getRunIteration(),
                task.getContentItemIdentity().value(),
                task.getTorrFilePath(),
                task.getTorrInfoHash(),
                task.getContentPath(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                null,
                null
        );
    }

    private DownloadContentProcess toEntity(@NonNull DownloadContentProcessDbRecord rec) {
        return DownloadContentProcess.builder()
                .id(rec.id())
                .status(DownloadContentProcessStatus.valueOf(rec.status()))
                .runIteration(rec.runIteration())
                .contentItemIdentity(new ContentItemIdentity(rec.contentItemIdentity()))
                .torrFilePath(rec.torrFilePath())
                .torrInfoHash(rec.torrInfoHash())
                .contentPath(rec.contentPath())
                .createdAt(rec.createdAt())
                .nextRunAfter(rec.nextRunAfter())
                .build();
    }
}
