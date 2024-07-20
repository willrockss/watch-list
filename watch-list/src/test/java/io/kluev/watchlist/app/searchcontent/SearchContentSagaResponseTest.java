package io.kluev.watchlist.app.searchcontent;

import io.kluev.watchlist.app.ChatMessageResponse;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SearchContentSagaResponseTest {

    @Test
    void should_parse_successfully() {
        // given
        val rawResp = new ChatMessageResponse("testChatId", "searchContentSaga_2eab2ae0-7701-40e7-ab43-e250758a4cd0_s_2");

        // when
        val resp = SearchContentSagaResponse.parseOrNull(rawResp);

        // then
        assertNotNull(resp);
        assertEquals(UUID.fromString("2eab2ae0-7701-40e7-ab43-e250758a4cd0"), resp.sagaId());
        assertEquals("2", resp.selectedContent());
    }
}