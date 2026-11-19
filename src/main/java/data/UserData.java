package data;

import database.db_connection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserData {

    public void SaveUser (long tgID, String username) throws SQLException {

        System.out.println("saving: " + tgID + " / " + username);
        String sql = """
                INSERT OR IGNORE INTO users (tg_id, username)
                VALUES(?, ?)
                """;
        try (Connection connection = db_connection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, tgID);
            statement.setString(2, username);

            int rows = statement.executeUpdate();

            System.out.println("rows changed: " + rows);

            statement.execute();
        }
    }
}
