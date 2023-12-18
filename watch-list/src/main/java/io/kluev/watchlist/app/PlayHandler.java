package io.kluev.watchlist.app;

import io.kluev.watchlist.infra.config.props.NodeRedIntegrationProperties;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class PlayHandler {

    // TODO Extract to separate facade-service
    private final RestClient restClient;
    private final NodeRedIntegrationProperties properties;
    public PlayResponse handle(PlayRequest request) {
        val resp = restClient
                .post()
                .uri(properties.getUrl() + "/play")
                .body(request)
                .retrieve()
                .toEntity(String.class);
        if (resp.getStatusCode().isError()) {
            return new PlayResponse(resp.getBody());
        }
        return new PlayResponse();
    }
}
