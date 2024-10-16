package io.kluev.watchlist.infra.jackett;

import io.kluev.watchlist.app.DownloadableContentInfo;
import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;


/**
 * This is not a real test. Just a playground with Jackett API
 */
@Disabled
@SuppressWarnings("unused")
@SpringBootTest(properties = {
        "integration.telegramBot.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "integration.telegram-bot.session-store-type=NOOP"
})
class JackettRestGatewayPlaygroundIT {

    @Autowired
    private JackettRestGateway jackettRestGateway;

    @Value("${USER}")
    private String user;

    @Test
    public void should_find_and_download_torr() {
        val query = "Ходячие мертвецы: Город мертвецов";
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
}