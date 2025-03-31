package io.kluev.watchlist.infra.config.management;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;

@Component
public class PidInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        try {
            String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
            builder.withDetail("PID", pid);
        } catch (Exception e) {
            builder.withDetail("PID-error", e.toString());
        }
    }
}