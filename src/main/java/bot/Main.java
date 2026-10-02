package bot;

import database.db_connection;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import service.movieService;
import service.collectionService;
import service.reviewService;


public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws TelegramApiException {

        String token = System.getenv("BOT_TOKEN");

        try {
            Connection connection = db_connection.getConnection();
            log.info("db started");
        } catch (SQLException e) {
            log.error("db not started", e);
        }

        TelegramBotsLongPollingApplication botsApplication =
                new TelegramBotsLongPollingApplication();

        movieService movieService = new movieService();
        collectionService collectionService = new collectionService();
        reviewService reviewService = new reviewService();

        CineMateBot bot = new CineMateBot(token, movieService, collectionService, reviewService);

        botsApplication.registerBot(token, bot);
        log.info("bot started");


    }
}
