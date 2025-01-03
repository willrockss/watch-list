package io.kluev.watchlist.infra.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> buildInfoPrometheusMetrics(BuildProperties buildProperties) {
        return registry -> Gauge.builder("app_build_info", 1.0, n -> n)
                .strongReference(true)
                .description("A metric with a constant ‘1’ value labeled by version, app_name, runtime version, and build time from which application was built.")
                .tags(
                        "app_version", buildProperties.getVersion(),
                        "app_name", buildProperties.getName(),
                        "build_time", buildProperties.getTime().toString()
                )
                .register(registry);
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> appNameMetricTagCustomization(
            @Value("${spring.application.name}") String appName
    ) {
        return registry -> registry
                .config()
                .commonTags("application", appName);
    }
}
