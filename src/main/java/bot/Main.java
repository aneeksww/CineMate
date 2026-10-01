package bot;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws TelegramApiException {

        String token = System.getenv("BOT_TOKEN");

        try {
            Connection connection = db_connection.getConnection();
            log.info("db started");
            connection.close();
        } catch (SQLException e) {
            log.error("db not started", e);
        }

        TelegramBotsLongPollingApplication botsApplication =
                new TelegramBotsLongPollingApplication();

        botsApplication.registerBot(token, new CineMateBot(token));

        log.info("bot started");


    }
}
