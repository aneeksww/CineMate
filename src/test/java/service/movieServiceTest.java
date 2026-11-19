package service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class movieServiceTest {

    movieService service = new movieService();
    collectionService collService = new collectionService();

    @Test
    void testGetBaseMoviesReturnsString() {
        String result = service.getBaseMovies();
        assertNotNull(result, "Ответ не должен быть null");
        assertFalse(result.isEmpty(), "Ответ не должен быть пустым");
    }

    @Test
    void testGetRandomFromBaseReturnsString() {
        String result = service.getRandomFromBase();
        assertNotNull(result, "Ответ не должен быть null");
        assertFalse(result.isEmpty(), "Ответ не должен быть пустым");
    }

    @Test
    void testAddMovieReturnsSuccessMessage() {
        String result = service.addMovie(1, "Тестовый фильм", 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetUserMoviesForEmptyUser() {
        String result = collService.getUserMovies(999999);
        assertNotNull(result);
        assertTrue(result.contains("нет фильмов") || result.contains("пуста"),
                "Должно вернуться сообщение о пустой подборке");
    }
}