package bot;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

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

            // ═══════════════════════════════════════
            //  CALLBACK (нажатия на кнопки)
            // ═══════════════════════════════════════
            if (update.hasCallbackQuery()) {

                String data = update.getCallbackQuery().getData();
                long ChatID = update.getCallbackQuery().getMessage().getChatId();
                long userIdLong = update.getCallbackQuery().getFrom().getId();

                // Главное меню
                if (data.equals("main_menu")) {
                    userStates.remove(userIdLong);
                    tempTitles.remove(userIdLong);

                    String helloText = "🏠 <b>Главное меню</b>\n\nКуда отправимся?";

                    EditMessageText message = EditMessageText.builder()
                            .chatId(ChatID)
                            .messageId(update.getCallbackQuery().getMessage().getMessageId())
                            .text(helloText)
                            .parseMode("HTML")
                            .replyMarkup(mainMenu())
                            .build();
                    try { telegramClient.execute(message); } catch (TelegramApiException e) { e.printStackTrace(); }
                    return;
                }

                // Роутинг по командам
                String answer = switch (data) {

                    case "add_movie" -> {
                        userStates.put(userIdLong, "AWAITING_TITLE");
                        yield "📝 <b>Введите название фильма:</b>\n<i>(или напишите /start для отмены)</i>";
                    }

                    case "cancel_add", "cancel_delete" -> {
                        userStates.remove(userIdLong);
                        tempTitles.remove(userIdLong);
                        yield "🚫 Действие отменено.";
                    }

                    case "genre_1", "genre_2", "genre_3", "genre_4",
                         "genre_5", "genre_6", "genre_7", "genre_8",
                         "genre_9", "genre_10" -> {
                        int genreId = Integer.parseInt(data.substring("genre_".length()));
                        String title = tempTitles.get(userIdLong);
                        if (title == null) yield "Сначала введите название фильма.";
                        String result = movieService.addMovie(userIdLong, title, genreId);
                        userStates.remove(userIdLong);
                        tempTitles.remove(userIdLong);
                        yield result;
                    }

                    case "delete_movie_menu" -> {
                        List<movie> userMovies = collectionService.getUserMoviesList(userIdLong);
                        if (userMovies.isEmpty()) {
                            yield "Твоя подборка пуста, нечего удалять.";
                        }
                        userStates.put(userIdLong, "AWAITING_DELETE");
                        yield "🗑 <b>Выберите фильм, который хотите удалить:</b>";
                    }

                    case "my_movies"    -> collectionService.getUserMovies(userIdLong);
                    case "random_mine"  -> collectionService.getRandomFromUser(userIdLong);
                    case "local_movies" -> movieService.getBaseMovies();
                    case "random_local" -> movieService.getRandomFromBase();
                    case "my_reviews"   -> reviewService.getUserReviews(userIdLong);

                    default -> {
                        if (data.startsWith("delete_")) {
                            int movieId = Integer.parseInt(data.substring("delete_".length()));
                            yield collectionService.deleteFromUserCollection(userIdLong, movieId);
                        }
                        yield "Неизвестная команда";
                    }
                };

                // Выбор клавиатуры
                InlineKeyboardMarkup keyboard;
                String currentState = userStates.get(userIdLong);

                if ("AWAITING_GENRE".equals(currentState)) {
                    keyboard = genreKeyboard();
                } else if ("AWAITING_DELETE".equals(currentState)) {
                    keyboard = deleteMovieKeyboard(userIdLong);
                } else if ("my_movies".equals(data)) {
                    keyboard = InlineKeyboardMarkup.builder()
                            .keyboardRow(new InlineKeyboardRow(
                                    InlineKeyboardButton.builder().text("🗑 Удалить фильм").callbackData("delete_movie_menu").build()
                            ))
                            .keyboardRow(new InlineKeyboardRow(
                                    InlineKeyboardButton.builder().text("⬅ Главное меню").callbackData("main_menu").build()
                            ))
                            .build();
                } else {
                    keyboard = InlineKeyboardMarkup.builder()
                            .keyboardRow(new InlineKeyboardRow(
                                    InlineKeyboardButton.builder().text("⬅ Главное меню").callbackData("main_menu").build()
                            ))
                            .build();
                }

                EditMessageText message = EditMessageText.builder()
                        .chatId(ChatID)
                        .messageId(update.getCallbackQuery().getMessage().getMessageId())
                        .text(answer)
                        .parseMode("HTML")
                        .replyMarkup(keyboard)
                        .build();

                try { telegramClient.execute(message); } catch (TelegramApiException e) { e.printStackTrace(); }
                return;
            }

            // ═══════════════════════════════════════
            //  MESSAGE (текст от пользователя)
            // ═══════════════════════════════════════
            if (update.hasMessage() && update.getMessage().hasText()) {

                String text = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();
                long userIdLong = update.getMessage().getFrom().getId();

                // Если ждем название фильма
                if ("AWAITING_TITLE".equals(userStates.get(userIdLong))) {
                    if (text.startsWith("/")) {
                        userStates.remove(userIdLong);
                        tempTitles.remove(userIdLong);
                        sendMsg(chatId, "🚫 Добавление отменено. Выбирай:", mainMenu());
                        return;
                    }

                    tempTitles.put(userIdLong, text);
                    userStates.put(userIdLong, "AWAITING_GENRE");
                    sendMsg(chatId, "🎭 Для фильма <b>«" + text + "»</b> выберите жанр:", genreKeyboard());
                    return;
                }

                // /start
                if (text.equals("/start")) {
                    String helloText = "👋 <b>Привет!</b> Я — <b>CineMate</b>.\n\n" +
                            "Твой личный кино-помощник. Здесь ты можешь собирать фильмы в свою подборку, " +
                            "искать случайные картины и оставлять отзывы.\n\n" +
                            "👇 <i>Выбирай действие в меню ниже:</i>";

                    // Отправляем картинку с текстом и меню
                    sendPhoto(chatId, "images/welcome.jpg", helloText, mainMenu());

                    try {
                        UserData.SaveUser(userIdLong, update.getMessage().getFrom().getUserName());
                    } catch (SQLException e) { e.printStackTrace(); }
                }
            }
        }
    }

    // Вспомогательный метод для отправки текста с HTML-разметкой
    private void sendMsg(long chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .replyMarkup(markup)
                .build();
        try { telegramClient.execute(msg); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    // Вспомогательный метод для отправки картинки с текстом и меню
    private void sendPhoto(long chatId, String imagePath, String caption, InlineKeyboardMarkup markup) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(imagePath);
        if (is == null) {
            // Если картинки нет, просто отправим текст
            sendMsg(chatId, caption, markup);
            return;
        }

        InputFile photo = new InputFile(is, "welcome.jpg");
        SendPhoto msg = SendPhoto.builder()
                .chatId(chatId)
                .photo(photo)
                .caption(caption)
                .parseMode("HTML")
                .replyMarkup(markup)
                .build();
        try { telegramClient.execute(msg); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    // Клавиатура главного меню (с эмодзи)
    private InlineKeyboardMarkup mainMenu() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("➕ Добавить фильм").callbackData("add_movie").build()))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("📚 Моя подборка").callbackData("my_movies").build(),
                        InlineKeyboardButton.builder().text("🎲 Случайный из подборки").callbackData("random_mine").build()
                ))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("🎬 База фильмов").callbackData("local_movies").build(),
                        InlineKeyboardButton.builder().text("🎰 Случайный из базы").callbackData("random_local").build()
                ))
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder().text("✍️ Мои отзывы").callbackData("my_reviews").build()))
                .build();
    }

    // Клавиатура жанров (с эмодзи)
    private InlineKeyboardMarkup genreKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("💥 Боевик").callbackData("genre_1").build(),
                        InlineKeyboardButton.builder().text("😂 Комедия").callbackData("genre_2").build()
                ))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("🎭 Драма").callbackData("genre_3").build(),
                        InlineKeyboardButton.builder().text("🚀 Фантастика").callbackData("genre_4").build()
                ))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("👻 Ужасы").callbackData("genre_5").build(),
                        InlineKeyboardButton.builder().text("🔪 Триллер").callbackData("genre_6").build()
                ))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("❤️ Мелодрама").callbackData("genre_7").build(),
                        InlineKeyboardButton.builder().text("🕵️ Детектив").callbackData("genre_8").build()
                ))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("❌ Отмена").callbackData("cancel_add").build()
                ))
                .build();
    }

    // Клавиатура со списком фильмов для удаления
    private InlineKeyboardMarkup deleteMovieKeyboard(long userId) {
        List<movie> movies = collectionService.getUserMoviesList(userId);
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();

        for (movie m : movies) {
            builder.keyboardRow(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text("❌ " + m.getTitle())
                            .callbackData("delete_" + m.getId())
                            .build()
            ));
        }
        builder.keyboardRow(new InlineKeyboardRow(
                InlineKeyboardButton.builder().text("⬅ Назад").callbackData("cancel_delete").build()
        ));

        return builder.build();
    }
}