package io.kluev.watchlist.app.searchcontent;

import io.kluev.watchlist.app.ChatMessageResponse;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record SearchContentSagaResponse(
    UUID sagaId,
    String selectedContent
) {

    private static final Pattern RESP_PATTERN = Pattern.compile("searchContentSaga_([0-9a-fA-F-]+)_s_(\\d+)");

    public static SearchContentSagaResponse parseOrNull(ChatMessageResponse response) {
        String input = response.responseText();
        Matcher m = RESP_PATTERN.matcher(input);

        if (!m.find()) {
            return null;
        }

        String sagaId = m.group(1);
        String index = m.group(2);
        System.out.println("sagaId: " + sagaId);
        System.out.println("index: " + index);

        return new SearchContentSagaResponse(UUID.fromString(sagaId), index);
    }
}
