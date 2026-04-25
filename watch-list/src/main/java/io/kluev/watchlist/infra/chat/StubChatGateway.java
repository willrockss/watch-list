package io.kluev.watchlist.infra.chat;

import io.kluev.watchlist.app.DownloadableContentInfo;
import io.kluev.watchlist.app.chat.ChatGateway;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
public class StubChatGateway implements ChatGateway {

    @PostConstruct
    public void init() {
        log.warn("Stub chat gateway is used! This should NEVER happen on production!");
    }

    @Override
    public void sendSelectContentRequest(UUID sagaId, List<DownloadableContentInfo> found) {
        log.info("sendSelectContentRequest {} {} will be ignored.", sagaId, found);
    }

    @Override
    public void sendMessage(MessageArgs args) {
        log.info("sendMessage {} {} will be ignored.", args.chatId(), args);
    }
}
