package service;

import database.db_connection;
import model.review;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class reviewService {

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
            return "У тебя пока нет отзывов.";
        }

        StringBuilder sb = new StringBuilder("✍ Твои отзывы:\n\n");
        for (review r : reviews) {
            sb.append("• ").append(r.getMovieTitle())
                    .append(" — ").append(r.getRating()).append("/10\n");
            if (r.getComment() != null && !r.getComment().isEmpty()) {
                sb.append("  ").append(r.getComment()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}