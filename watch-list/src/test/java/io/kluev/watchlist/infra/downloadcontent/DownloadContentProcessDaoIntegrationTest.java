package io.kluev.watchlist.infra.downloadcontent;

import io.kluev.watchlist.app.downloadcontent.ContentItemIdentity;
import io.kluev.watchlist.app.downloadcontent.DownloadContentProcess;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.stream.Collectors;

import static io.kluev.watchlist.app.downloadcontent.DownloadContentProcessStatus.INITIAL;
import static io.kluev.watchlist.app.downloadcontent.DownloadContentProcessStatus.PROCESSING;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("IntegrationTest")
@Testcontainers
@Transactional
@SpringBootTest
class DownloadContentProcessDaoIntegrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private DownloadContentProcessDao downloadContentProcessDao;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQL = new PostgreSQLContainer<>("postgres:14");


    @Test
    void test_save_process() {
        // given
        val process = DownloadContentProcess
                .builder()
                .contentItemIdentity(new ContentItemIdentity("kinopoisk-123"))
                .torrFilePath("/tmp/some_file.torr")
                .build();

        // when
        downloadContentProcessDao.save(process);

        // then
        val result = jdbcClient.sql("SELECT * FROM download_content_process WHERE content_item_identity = 'kinopoisk-123'")
                .query(DownloadContentTaskDbRecordRowMapper.INSTANCE)
                .optional()
                .orElse(null);

        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();
        assertThat(result.runIteration()).isZero();
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.status()).isEqualTo(INITIAL.name());
        assertThat(result.contentItemIdentity()).isEqualTo("kinopoisk-123");
        assertThat(result.torrFilePath()).isEqualTo("/tmp/some_file.torr");
    }

    @Sql("/sql/add_download_processes.sql")
    @Test
    void test_load_for_processing() {
        // given -> setup Sql
        // when
        val result = downloadContentProcessDao.getActive();

        // then
        assertThat(result).hasSize(2);

        val statuses = result.stream().map(DownloadContentProcess::getStatus).collect(Collectors.toSet());
        assertThat(statuses).containsOnly(INITIAL, PROCESSING);

        for (DownloadContentProcess downloadContentProcess : result) {
            assertThat(downloadContentProcess.getCreatedAt()).isNotNull();
            assertThat(downloadContentProcess.getNextRunAfter()).isNotNull();
        }
    }

}