package io.kluev.watchlist.infra.googlesheet;

import io.kluev.watchlist.app.SeriesDto;
import io.kluev.watchlist.app.SeriesRepository;
import io.kluev.watchlist.domain.Series;
import io.kluev.watchlist.domain.WatchDateStrategy;
import io.kluev.watchlist.domain.event.SeasonWatched;
import io.kluev.watchlist.infra.googlesheet.clientimpl.CutInsertWithUpdateRowCommand;
import io.kluev.watchlist.infra.googlesheet.clientimpl.FindRowByValues;
import io.kluev.watchlist.infra.googlesheet.clientimpl.GoogleSheetsClient;
import io.kluev.watchlist.infra.googlesheet.clientimpl.RowCellValue;
import io.kluev.watchlist.infra.googlesheet.clientimpl.RowNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class GoogleSheetsSeriesRepository implements SeriesRepository {

    public static final String IN_PROGRESS_SEASONS_SHEET_CODE = "series";
    public static final String WATCHED_SEASONS_SHEET_CODE = "watchedSeries";

    private final GoogleSheetsClient client;
    private final WatchDateStrategy watchDateStrategy;

    @Override
    public List<SeriesDto> getAllInProgress() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public Optional<Series> getInProgressById(String seriesId) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void save(Series series) {
        val seasonWatchedEvent = (SeasonWatched) series.getEvents()
                .stream()
                .filter(it -> it instanceof SeasonWatched)
                .findFirst()
                .orElse(null);

        if (seasonWatchedEvent == null) {
            log.info("Current implementation can process only SeasonWatched use-case");
            return;
        }

        log.info("Going to mark {} watched.", seasonWatchedEvent);
        RowNumber seriesSeasonRowNumber = client.findRow(
                new FindRowByValues(
                        IN_PROGRESS_SEASONS_SHEET_CODE,
                        List.of(
                                new RowCellValue("name", series.getTitle()),
                                new RowCellValue("seasonNumber", series.getSeasonNumber().toString())
                        )
                )
        );
        Assert.state(seriesSeasonRowNumber != null, "Cannot find " + series + ". Unable to mark watched");

        String watchedFullTitle = "%s (%d, %d/%d)".formatted(
                series.getTitle(), series.getSeasonNumber(),
                series.getLastWatchedEpisodeNumber(), series.getEpisodes().size()
        );
        client.cutInsertWithUpdate(
                CutInsertWithUpdateRowCommand
                        .builder()
                        .sheetCodeCutFrom(IN_PROGRESS_SEASONS_SHEET_CODE)
                        .cutRowNumber(seriesSeasonRowNumber)
                        .sheetCodeInsertInto(WATCHED_SEASONS_SHEET_CODE)
                        .insertRowNumber(RowNumber.FIRST_AFTER_HEADER)
                        .updatedFields(Map.of(
                                "name", watchedFullTitle,
                                "watchedAt", watchDateStrategy.calculateWatchDate()
                        ))
                        .build()
        );
    }
}
