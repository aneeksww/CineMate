package org.example;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.sql.Connection;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) throws TelegramApiException {

        String token = System.getenv("BOT_TOKEN");

        try {
            Connection connection = db_connection.getConnection();
            System.out.println("БД подключена!");
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        TelegramBotsLongPollingApplication botsApplication =
                new TelegramBotsLongPollingApplication();

        botsApplication.registerBot(token, new CineMateBot(token));

        System.out.println("Бот запущен!");


    }
}
