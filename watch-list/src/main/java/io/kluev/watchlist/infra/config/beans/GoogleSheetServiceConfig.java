package io.kluev.watchlist.infra.config.beans;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.util.List;

@Configuration
public class GoogleSheetServiceConfig {

    @Value("${integration.google.credentialsFile}")
    private String googleCredentialsFile;

    public static final String SHEET_APP_NAME = "WatchList";
    private static final List<String> SCOPES = List.of(SheetsScopes.SPREADSHEETS);

    @SneakyThrows
    @Bean
    Sheets googleSheetsService() {
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        var credential = GoogleCredential.fromStream(new FileInputStream(googleCredentialsFile))
                .createScoped(SCOPES);
        return new Sheets.Builder(httpTransport, JacksonFactory.getDefaultInstance(), credential)
                .setApplicationName(SHEET_APP_NAME)
                .build();
    }
}
