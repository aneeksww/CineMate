package service;

import database.db_connection;
import model.movie;
import model.review;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class reviewService {

    // Получить все отзывы пользователя
    public String getUserReviews(long userId) {
        String sql = "SELECT r.id, m.title, r.rating, r.comment, r.rate_date " +
                "FROM reviews r " +
                "JOIN movies m ON m.id = r.movie_id " +
                "WHERE r.tg_id = ? " +
                "ORDER BY r.rate_date DESC";
        List<review> reviews = new ArrayList<>();

        try (Connection connection = db_connection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                reviews.add(new review(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getInt("rating"),
                        rs.getString("comment"),
                        rs.getString("rate_date")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Ошибка при получении отзывов.";
        }

        if (reviews.isEmpty()) {
            return "✍️ У тебя пока нет отзывов.";
        }

        StringBuilder sb = new StringBuilder("✍️ <b>Твои отзывы:</b>\n\n");
        for (review r : reviews) {
            sb.append("🎬 <b>").append(r.getMovieTitle()).append("</b>\n")
                    .append("⭐ Оценка: <b>").append(r.getRating()).append("/10</b>\n");
            if (r.getComment() != null && !r.getComment().isEmpty()) {
                sb.append("💬 ").append(r.getComment()).append("\n");
            }
            if (r.getRateDate() != null) {
                sb.append("<i>📅 ").append(r.getRateDate()).append("</i>\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // Сохранение нового отзыва
    public String addReview(long userId, int movieId, int rating, String comment) {
        String sql = "INSERT INTO reviews (tg_id, movie_id, rating, comment) VALUES (?, ?, ?, ?)";
        try (Connection connection = db_connection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setInt(2, movieId);
            stmt.setInt(3, rating);
            stmt.setString(4, comment);
            stmt.executeUpdate();
            return "✅ Спасибо! Твой отзыв сохранен.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Ошибка при сохранении отзыва.";
        }
    }

    // Получить фильмы из подборки пользователя (для оценки)
    public List<movie> getUserMoviesForReview(long userId) {
        String sql = "SELECT m.id, m.title FROM movies m " +
                "JOIN user_movies um ON um.movie_id = m.id " +
                "WHERE um.tg_id = ?";
        List<movie> movies = new ArrayList<>();
        try (Connection connection = db_connection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                movies.add(new movie(rs.getInt("id"), rs.getString("title"), "", ""));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movies;
    }
}