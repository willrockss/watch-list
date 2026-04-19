package io.kluev.watchlist.infra.telegrambot;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.ObjectUtils;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.starter.TelegramBotInitializer;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Slf4j
public class LazyTelegramBotInitializer extends TelegramBotInitializer {

    public LazyTelegramBotInitializer(TelegramBotsLongPollingApplication telegramBotsApplication, List<SpringLongPollingBot> longPollingBots) {
        super(telegramBotsApplication, ObjectUtils.defaultIfNull(longPollingBots, List.of()));
    }

    @SuppressWarnings("BusyWait") // Ignore for VirtualThread
    @SneakyThrows
    @Override
    public void afterPropertiesSet() {
        val initialized = new AtomicBoolean(false);
        Thread.startVirtualThread(() -> {
             do {
                try {
                    super.afterPropertiesSet();
                    initialized.set(true);
                } catch (Exception e) {
                    if (isAlreadyInitializedException(e)) {
                        initialized.set(true);
                    } else {
                        log.warn("Unable to initialize TG bot due to {}. Retry", e.toString());
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            } while (Thread.currentThread().isAlive() && !initialized.get());
        });
    }

    /**
     * This would work for single bot only. Rewrite initializer in case of multiple
     */
    private boolean isAlreadyInitializedException(Exception e) {
        if (e.getCause() != null && e.getCause() instanceof TelegramApiException apiException) {
            if ("Bot is already registered".equals(apiException.getMessage())) {
                log.info("Some bots are already initialized successfully. Ignore exception");
                return true;
            }
        }
        return false;
    }

}
