package service;

import database.db_connection;
import model.movie;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class collectionService {

    // 1. Получить список фильмов пользователя (для вывода текстом)
    public String getUserMovies(long userId) {
        String sql = "SELECT m.id, m.title, " +
                "(SELECT g.name FROM movie_genres mg " +
                " JOIN genres g ON g.id = mg.genre_id " +
                " WHERE mg.movie_id = m.id LIMIT 1) AS genre " +
                "FROM movies m " +
                "JOIN user_movies um ON um.movie_id = m.id " +
                "WHERE um.tg_id = ?";
        List<movie> movies = new ArrayList<>();

        try (Connection connection = db_connection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

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
            return "Ошибка при получении подборки.";
        }

        if (movies.isEmpty()) {
            return "У тебя пока нет фильмов в подборке.";
        }

        StringBuilder sb = new StringBuilder("Твоя подборка:\n\n");
        for (movie m : movies) {
            sb.append("• ").append(m.getTitle())
                    .append(" (").append(m.getGenre()).append(")\n");
        }
        return sb.toString();
    }

    // 2. Получить случайный фильм из подборки
    public String getRandomFromUser(long userId) {
        String sql = "SELECT m.id, m.title, " +
                "(SELECT g.name FROM movie_genres mg " +
                " JOIN genres g ON g.id = mg.genre_id " +
                " WHERE mg.movie_id = m.id LIMIT 1) AS genre " +
                "FROM movies m " +
                "JOIN user_movies um ON um.movie_id = m.id " +
                "WHERE um.tg_id = ?";
        List<movie> movies = new ArrayList<>();

        try (Connection connection = db_connection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

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
            return "Подборка пуста — нечего выбирать.";
        }

        movie m = movies.get(new Random().nextInt(movies.size()));
        return "Случайный из твоей подборки:\n" + m.getTitle() + " (" + m.getGenre() + ")";
    }

    // 3. Удаление фильма из подборки
    public String deleteFromUserCollection(long userId, int movieId) {
        String sql = "DELETE FROM user_movies WHERE tg_id = ? AND movie_id = ?";
        try (Connection connection = db_connection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setInt(2, movieId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                return "🗑 Фильм удален из подборки.";
            } else {
                return "Фильм не найден в подборке.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Ошибка при удалении фильма.";
        }
    }

    // 4. Получить список фильмов в виде List (для генерации кнопок удаления)
    public List<movie> getUserMoviesList(long userId) {
        String sql = "SELECT m.id, m.title, " +
                "(SELECT g.name FROM movie_genres mg " +
                " JOIN genres g ON g.id = mg.genre_id " +
                " WHERE mg.movie_id = m.id LIMIT 1) AS genre " +
                "FROM movies m " +
                "JOIN user_movies um ON um.movie_id = m.id " +
                "WHERE um.tg_id = ?";
        List<movie> movies = new ArrayList<>();

        try (Connection connection = db_connection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
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
        }
        return movies;
    }
}