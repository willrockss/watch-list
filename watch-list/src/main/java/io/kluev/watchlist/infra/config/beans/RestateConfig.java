package io.kluev.watchlist.infra.config.beans;

import dev.restate.sdk.springboot.EnableRestate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "workflow-engine", havingValue = "RESTATE")
@EnableRestate
public class RestateConfig {
}
