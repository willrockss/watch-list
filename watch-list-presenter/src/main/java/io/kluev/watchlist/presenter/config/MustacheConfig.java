package io.kluev.watchlist.presenter.config;

import com.samskivert.mustache.Mustache;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MustacheConfig {

    @Bean
    public Mustache.Compiler mustacheCompiler(
            Mustache.TemplateLoader templateLoader
    ) {
        return Mustache
                .compiler()
                .defaultValue("null")
                .withLoader(templateLoader);
    }
}
