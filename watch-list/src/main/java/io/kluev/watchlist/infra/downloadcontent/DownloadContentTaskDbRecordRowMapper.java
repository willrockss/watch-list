package io.kluev.watchlist.infra.downloadcontent;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DownloadContentTaskDbRecordRowMapper implements RowMapper<DownloadContentProcessDbRecord> {

    public static final DownloadContentTaskDbRecordRowMapper INSTANCE = new DownloadContentTaskDbRecordRowMapper();

    @Override
    public DownloadContentProcessDbRecord mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return DownloadContentProcessDbRecord.builder()
                .id(rs.getLong("id"))
                .status(rs.getString("status"))
                .runIteration(rs.getInt("run_iteration"))
                .contentItemIdentity(rs.getString("content_item_identity"))
                .torrFilePath(rs.getString("torr_file_path"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .nextRunAfter(rs.getObject("next_run_after", OffsetDateTime.class))
                .context(rs.getBytes("context"))
                .build();
    }
}
