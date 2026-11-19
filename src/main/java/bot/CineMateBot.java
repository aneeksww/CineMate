package bot;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.UserData;
import model.movie;
import service.collectionService;
import service.movieService;
import service.reviewService;

public class CineMateBot implements LongPollingUpdateConsumer {

    private final TelegramClient telegramClient;
    private final UserData UserData;
    private final movieService movieService;
    private final collectionService collectionService;
    private final reviewService reviewService;

    // FSM
    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, String> tempTitles = new HashMap<>();
    private final Map<Long, Integer> tempMovieIds = new HashMap<>(); // для оценки
    private final Map<Long, Integer> tempRatings = new HashMap<>();  // для оценки

    public CineMateBot(String token,
                       movieService movieService,
                       collectionService collectionService,
                       reviewService reviewService) {
        telegramClient = new OkHttpTelegramClient(token);
        UserData = new UserData();
        this.movieService = movieService;
        this.collectionService = collectionService;
        this.reviewService = reviewService;
    }

    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) {

            if (update.hasCallbackQuery()) {
                String data = update.getCallbackQuery().getData();
                long ChatID = update.getCallbackQuery().getMessage().getChatId();
                int messageId = update.getCallbackQuery().getMessage().getMessageId();
                long userIdLong = update.getCallbackQuery().getFrom().getId();


                deleteMessage(ChatID, messageId);

                // кнопка главного меню
                if (data.equals("main_menu")) {
                    userStates.remove(userIdLong);
                    tempTitles.remove(userIdLong);
                    sendPhoto(ChatID, "images/welcome.jpg", "🏠 <b>Главное меню</b>\n\nКуда отправимся?", mainMenu());
                    return;
                }

                // кнопки меню
                switch (data) {
                    case "add_movie" -> {
                        userStates.put(userIdLong, "AWAITING_TITLE");
                        sendPhoto(ChatID, "images/add_movie.jpg", "📝 <b>Введите название фильма:</b>\n<i>(или /start для отмены)</i>", backKeyboard());
                    }
                    case "my_movies" -> {
                        String text = collectionService.getUserMovies(userIdLong);
                        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("🗑 Удалить фильм").callbackData("delete_movie_menu").build()))
                                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("⬅ Главное меню").callbackData("main_menu").build()))
                                .build();
                        sendPhoto(ChatID, "images/my_movies.jpg", text, kb);
                    }
                    case "random_mine" -> sendPhoto(ChatID, "images/random_mine.jpg", collectionService.getRandomFromUser(userIdLong), backKeyboard());
                    case "local_movies" -> sendPhoto(ChatID, "images/base_movies.jpg", movieService.getBaseMovies(), backKeyboard());
                    case "random_local" -> sendPhoto(ChatID, "images/random_base.jpg", movieService.getRandomFromBase(), backKeyboard());

                    case "cancel_add", "cancel_delete", "cancel_review" -> {
                        userStates.remove(userIdLong);
                        tempTitles.remove(userIdLong);
                        sendPhoto(ChatID, "images/welcome.jpg", "🚫 Действие отменено.", mainMenu());
                    }

                    // отзывы(хз кому оно надо,аня сказала)
                    case "my_reviews" -> {
                        String text = reviewService.getUserReviews(userIdLong);
                        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("➕ Написать отзыв").callbackData("write_review").build()))
                                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("⬅ Главное меню").callbackData("main_menu").build()))
                                .build();
                        sendPhoto(ChatID, "images/my_reviews.jpg", text, kb);
                    }
                    case "write_review" -> {
                        List<movie> movies = reviewService.getUserMoviesForReview(userIdLong);
                        if (movies.isEmpty()) {
                            sendPhoto(ChatID, "images/my_reviews.jpg", "У тебя нет фильмов в подборке, нечего оценивать.", backKeyboard());
                        } else {
                            userStates.put(userIdLong, "AWAITING_REVIEW_MOVIE");
                            sendPhoto(ChatID, "images/my_reviews.jpg", "🎬 Выберите фильм для отзыва:", reviewMovieKeyboard(movies));
                        }
                    }
                    case "delete_movie_menu" -> {
                        List<movie> userMovies = collectionService.getUserMoviesList(userIdLong);
                        if (userMovies.isEmpty()) {
                            sendPhoto(ChatID, "images/my_movies.jpg", "Твоя подборка пуста, нечего удалять.", backKeyboard());
                        } else {
                            userStates.put(userIdLong, "AWAITING_DELETE");
                            sendPhoto(ChatID, "images/my_movies.jpg", "🗑 <b>Выберите фильм, который хотите удалить:</b>", deleteMovieKeyboard(userIdLong));
                        }
                    }

                    // кнопки динамические
                    default -> {
                        if (data.startsWith("genre_")) {
                            int genreId = Integer.parseInt(data.substring("genre_".length()));
                            String title = tempTitles.get(userIdLong);
                            if (title != null) {
                                String result = movieService.addMovie(userIdLong, title, genreId);
                                userStates.remove(userIdLong);
                                tempTitles.remove(userIdLong);
                                sendPhoto(ChatID, "images/welcome.jpg", result, mainMenu());
                            }
                        } else if (data.startsWith("revmovie_")) {
                            int movieId = Integer.parseInt(data.substring("revmovie_".length()));
                            tempMovieIds.put(userIdLong, movieId);
                            userStates.put(userIdLong, "AWAITING_REVIEW_RATING");
                            sendPhoto(ChatID, "images/my_reviews.jpg", "⭐ Поставьте оценку фильму от 1 до 10:", ratingKeyboard());
                        } else if (data.startsWith("rating_")) {
                            int rating = Integer.parseInt(data.substring("rating_".length()));
                            tempRatings.put(userIdLong, rating);
                            userStates.put(userIdLong, "AWAITING_REVIEW_COMMENT");
                            sendMsg(ChatID, "✍️ Напишите ваш комментарий к фильму:", backKeyboard());
                        } else if (data.startsWith("delete_")) {
                            int movieId = Integer.parseInt(data.substring("delete_".length()));
                            sendPhoto(ChatID, "images/my_movies.jpg", collectionService.deleteFromUserCollection(userIdLong, movieId), backKeyboard());
                        }
                    }
                }
                return;
            }

            // текст от пользлвателя
            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();
                long userIdLong = update.getMessage().getFrom().getId();

                if ("AWAITING_TITLE".equals(userStates.get(userIdLong))) {
                    if (text.startsWith("/")) {
                        userStates.remove(userIdLong);
                        tempTitles.remove(userIdLong);
                        sendPhoto(chatId, "images/welcome.jpg", "🚫 Добавление отменено.", mainMenu());
                        return;
                    }
                    tempTitles.put(userIdLong, text);
                    userStates.put(userIdLong, "AWAITING_GENRE");
                    sendPhoto(chatId, "images/add_movie.jpg", "🎭 Для фильма <b>«" + text + "»</b> выберите жанр:", genreKeyboard());
                    return;
                }

                //  комментарий к отзыву
                if ("AWAITING_REVIEW_COMMENT".equals(userStates.get(userIdLong))) {
                    int movieId = tempMovieIds.get(userIdLong);
                    int rating = tempRatings.get(userIdLong);
                    String result = reviewService.addReview(userIdLong, movieId, rating, text);

                    userStates.remove(userIdLong);
                    tempMovieIds.remove(userIdLong);
                    tempRatings.remove(userIdLong);

                    sendPhoto(chatId, "images/welcome.jpg", result, mainMenu());
                    return;
                }

                // /start
                if (text.equals("/start")) {
                    String helloText = "👋 <b>Привет!</b> Я — <b>CineMate</b>.\n\n" +
                            "Твой личный кино-помощник. Здесь ты можешь собирать фильмы в свою подборку, " +
                            "искать случайные картины и оставлять отзывы.\n\n" +
                            "👇 <i>Выбирай действие в меню ниже:</i>";
                    sendPhoto(chatId, "images/welcome.jpg", helloText, mainMenu());
                    try { UserData.SaveUser(userIdLong, update.getMessage().getFrom().getUserName()); } catch (SQLException e) { e.printStackTrace(); }
                }
            }
        }
    }

    private void sendMsg(long chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage msg = SendMessage.builder().chatId(chatId).text(text).parseMode("HTML").replyMarkup(markup).build();
        try { telegramClient.execute(msg); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    private void sendPhoto(long chatId, String imagePath, String caption, InlineKeyboardMarkup markup) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(imagePath);
        if (is == null) { sendMsg(chatId, caption, markup); return; }
        InputFile photo = new InputFile(is, imagePath);
        SendPhoto msg = SendPhoto.builder().chatId(chatId).photo(photo).caption(caption).parseMode("HTML").replyMarkup(markup).build();
        try { telegramClient.execute(msg); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    private void deleteMessage(long chatId, int messageId) {
        try { telegramClient.execute(new DeleteMessage(String.valueOf(chatId), messageId)); } catch (TelegramApiException e) {}
    }

    private InlineKeyboardMarkup mainMenu() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("➕ Добавить фильм").callbackData("add_movie").build()))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("📚 Моя подборка").callbackData("my_movies").build(),
                        InlineKeyboardButton.builder().text("🎲 Случайный из подборки").callbackData("random_mine").build()))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("🎬 База фильмов").callbackData("local_movies").build(),
                        InlineKeyboardButton.builder().text("🎰 Случайный из базы").callbackData("random_local").build()))
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("✍️ Мои отзывы").callbackData("my_reviews").build()))
                .build();
    }

    private InlineKeyboardMarkup genreKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("💥 Боевик").callbackData("genre_1").build(), InlineKeyboardButton.builder().text("😂 Комедия").callbackData("genre_2").build()))
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("🎭 Драма").callbackData("genre_3").build(), InlineKeyboardButton.builder().text("🚀 Фантастика").callbackData("genre_4").build()))
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("👻 Ужасы").callbackData("genre_5").build(), InlineKeyboardButton.builder().text("🔪 Триллер").callbackData("genre_6").build()))
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("❤️ Мелодрама").callbackData("genre_7").build(), InlineKeyboardButton.builder().text("🕵️ Детектив").callbackData("genre_8").build()))
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("❌ Отмена").callbackData("cancel_add").build()))
                .build();
    }

    private InlineKeyboardMarkup deleteMovieKeyboard(long userId) {
        List<movie> movies = collectionService.getUserMoviesList(userId);
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();
        for (movie m : movies) {
            builder.keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("❌ " + m.getTitle()).callbackData("delete_" + m.getId()).build()));
        }
        builder.keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("⬅ Назад").callbackData("cancel_delete").build()));
        return builder.build();
    }

    private InlineKeyboardMarkup reviewMovieKeyboard(List<movie> movies) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();
        for (movie m : movies) {
            builder.keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("🎬 " + m.getTitle()).callbackData("revmovie_" + m.getId()).build()));
        }
        builder.keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("⬅ Назад").callbackData("cancel_review").build()));
        return builder.build();
    }

    private InlineKeyboardMarkup ratingKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("1").callbackData("rating_1").build(), InlineKeyboardButton.builder().text("2").callbackData("rating_2").build(), InlineKeyboardButton.builder().text("3").callbackData("rating_3").build(), InlineKeyboardButton.builder().text("4").callbackData("rating_4").build(), InlineKeyboardButton.builder().text("5").callbackData("rating_5").build()))
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("6").callbackData("rating_6").build(), InlineKeyboardButton.builder().text("7").callbackData("rating_7").build(), InlineKeyboardButton.builder().text("8").callbackData("rating_8").build(), InlineKeyboardButton.builder().text("9").callbackData("rating_9").build(), InlineKeyboardButton.builder().text("10").callbackData("rating_10").build()))
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("❌ Отмена").callbackData("cancel_review").build()))
                .build();
    }

    private InlineKeyboardMarkup backKeyboard() {
        return InlineKeyboardMarkup.builder().keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("⬅ Главное меню").callbackData("main_menu").build())).build();
    }
}