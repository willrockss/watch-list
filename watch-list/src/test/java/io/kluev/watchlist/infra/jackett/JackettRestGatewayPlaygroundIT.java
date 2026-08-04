package io.kluev.watchlist.infra.jackett;

import io.kluev.watchlist.app.DownloadableContentInfo;
import io.kluev.watchlist.infra.config.props.JackettProperties;
import lombok.val;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import org.apache.hc.client5.http.impl.classic.HttpClients;


/**
 * This is not a real test. Just a playground with Jackett API
 */
@Disabled
@EnableConfigurationProperties(JackettProperties.class)
@SuppressWarnings("unused")
@SpringBootTest(properties = {
        "integration.jackett.base-url=http://localhost:9117",
        "integration.jackett.api-key=123",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "integration.telegram-bot.session-store-type=NOOP"
}, classes = {JackettRestGateway.class, JackettRestGatewayPlaygroundIT.TestConfig.class})
class JackettRestGatewayPlaygroundIT {

    @Autowired
    private JackettRestGateway jackettRestGateway;

    @Value("${USER}")
    private String user;

    @Test
    public void should_find_and_download_torr() {
        val query = "Терминатор 2";
        val response = jackettRestGateway.query(query);
        System.out.println(response);
        for (DownloadableContentInfo con : response) {
            System.out.println(con.getLink());
        }

//        val torrFileResource = jackettRestGateway.download(response.getFirst());
//        val file = Path.of("/home", user, "Downloads", "torr", torrFileResource.filename()).toFile();
//
//        try {
//            FileUtils.writeByteArrayToFile(file, torrFileResource.bytes());
//            System.out.println(file.getCanonicalFile() + " was created");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @TestConfiguration
    public static class TestConfig {
        @Bean
        public RestClient restClient() {
            // 2. Настраиваем таймауты подключения и ответа сервера
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(Timeout.ofMinutes(2))       // Время на установку соединения
                    .setResponseTimeout(Timeout.ofMinutes(2))      // Время ожидания ответа (Read Timeout)
                    .setConnectionRequestTimeout(Timeout.ofMinutes(2)) // Время ожидания соединения из пула
                    .build();

            // 3. Создаем HttpClient с нашими настройками
            HttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .build();
            return RestClient.builder()
                    .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                    .build();
        }
    }
}