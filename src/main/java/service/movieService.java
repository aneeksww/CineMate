package service;

import database.db_connection;
import model.movie;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class movieService {

    public String startAddMovie(long userId) {
        return "Введите название фильма:";
    }

    public String getBaseMovies() {
        String sql = "SELECT m.id, m.title, " +
                "(SELECT g.name FROM movie_genres mg " +
                " JOIN genres g ON g.id = mg.genre_id " +
                " WHERE mg.movie_id = m.id LIMIT 1) AS genre " +
                "FROM movies m " +
                "WHERE m.is_base = 1";
        List<movie> movies = new ArrayList<>();

        try (Connection connection = db_connection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                movies.add(new movie(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("genre"),
                        ""
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Ошибка при получении базы фильмов.";
        }

        if (movies.isEmpty()) {
            return "База фильмов пуста.";
        }

        StringBuilder sb = new StringBuilder("Культовая база:\n\n");
        for (movie m : movies) {
            sb.append("• ").append(m.getTitle())
                    .append(" (").append(m.getGenre()).append(")\n");
        }
        return sb.toString();
    }

    public String getRandomFromBase() {
        String sql = "SELECT m.id, m.title, " +
                "(SELECT g.name FROM movie_genres mg " +
                " JOIN genres g ON g.id = mg.genre_id " +
                " WHERE mg.movie_id = m.id LIMIT 1) AS genre " +
                "FROM movies m " +
                "WHERE m.is_base = 1";
        List<movie> movies = new ArrayList<>();

        try (Connection connection = db_connection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                movies.add(new movie(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("genre"),
                        ""
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Ошибка при выборе случайного фильма.";
        }

        if (movies.isEmpty()) {
            return "База фильмов пуста.";
        }

        movie m = movies.get(new Random().nextInt(movies.size()));
        return "Случайный из базы:\n" + m.getTitle() + " (" + m.getGenre() + ")";
    }

    // ─── Добавление фильма в подборку пользователя ───
    public String addMovie(long userId, String title, int genreId) {
        try {
            int movieId;

            // 1. Вставить фильм (is_base = 0, потому что пользовательский)
            String sql1 = "INSERT INTO movies (title, is_base) VALUES (?, 0)";
            try (Connection conn = db_connection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         sql1, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, title);
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    keys.next();
                    movieId = keys.getInt(1);
                }
            }

            // 2. Связать с жанром
            String sql2 = "INSERT OR IGNORE INTO movie_genres (movie_id, genre_id) VALUES (?, ?)";
            try (Connection conn = db_connection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql2)) {
                stmt.setInt(1, movieId);
                stmt.setInt(2, genreId);
                stmt.executeUpdate();
            }

            // 3. Добавить в подборку пользователя
            String sql3 = "INSERT OR IGNORE INTO user_movies (tg_id, movie_id) VALUES (?, ?)";
            try (Connection conn = db_connection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql3)) {
                stmt.setLong(1, userId);
                stmt.setInt(2, movieId);
                stmt.executeUpdate();
            }

            return "✅ Фильм «" + title + "» добавлен в твою подборку!";

        } catch (SQLException e) {
            e.printStackTrace();
            return "Ошибка при добавлении фильма.";
        }
    }
}