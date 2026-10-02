package bot;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.UserData;
import service.collectionService;
import service.movieService;
import service.reviewService;

public class CineMateBot implements LongPollingUpdateConsumer {

    private final TelegramClient telegramClient;
    private final UserData UserData;
    private final movieService movieService;
    private final collectionService collectionService;
    private final reviewService reviewService;

    private final Map<Long, String> userStates = new HashMap<>();  // userId → "AWAITING_TITLE" / "AWAITING_GENRE"
    private final Map<Long, String> tempTitles = new HashMap<>();  // userId → введённое название

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
                long userIdLong = update.getCallbackQuery().getFrom().getId();

                if (data.equals("main_menu")) {
                    userStates.remove(userIdLong);
                    tempTitles.remove(userIdLong);

                    EditMessageText message = EditMessageText.builder()
                            .chatId(ChatID)
                            .messageId(update.getCallbackQuery().getMessage().getMessageId())
                            .text("Выбирай:")
                            .replyMarkup(mainMenu())
                            .build();
                    try {
                        telegramClient.execute(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    return;
                }

                String answer = switch (data) {

                    case "add_movie" -> {
                        userStates.put(userIdLong, "AWAITING_TITLE");
                        yield "Введите название фильма:";
                    }

                    case "genre_1", "genre_2", "genre_3", "genre_4",
                         "genre_5", "genre_6", "genre_7", "genre_8",
                         "genre_9", "genre_10" -> {
                        int genreId = Integer.parseInt(data.substring("genre_".length()));
                        String title = tempTitles.get(userIdLong);

                        if (title == null) {
                            yield "Сначала введите название фильма.";
                        }

                        String result = movieService.addMovie(userIdLong, title, genreId);

                        userStates.remove(userIdLong);
                        tempTitles.remove(userIdLong);

                        yield result;
                    }

                    case "my_movies"    -> collectionService.getUserMovies(userIdLong);
                    case "random_mine"  -> collectionService.getRandomFromUser(userIdLong);
                    case "local_movies" -> movieService.getBaseMovies();
                    case "random_local" -> movieService.getRandomFromBase();
                    case "my_reviews"   -> reviewService.getUserReviews(userIdLong);
                    default             -> "Неизвестная команда";
                };

                InlineKeyboardMarkup keyboard;
                if ("AWAITING_GENRE".equals(userStates.get(userIdLong))) {
                    keyboard = genreKeyboard();
                } else {
                    keyboard = InlineKeyboardMarkup.builder()
                            .keyboardRow(new InlineKeyboardRow(
                                    InlineKeyboardButton.builder()
                                            .text("⬅ Главное меню")
                                            .callbackData("main_menu")
                                            .build()
                            ))
                            .build();
                }

                EditMessageText message = EditMessageText.builder()
                        .chatId(ChatID)
                        .messageId(update.getCallbackQuery().getMessage().getMessageId())
                        .text(answer)
                        .replyMarkup(keyboard)
                        .build();

                try {
                    telegramClient.execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                return;
            }


            if (update.hasMessage() && update.getMessage().hasText()) {

                String text = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();
                long userIdLong = update.getMessage().getFrom().getId();

                // ─── FSM: если ждём название фильма ───
                if ("AWAITING_TITLE".equals(userStates.get(userIdLong))) {
                    tempTitles.put(userIdLong, text);
                    userStates.put(userIdLong, "AWAITING_GENRE");

                    SendMessage msg = SendMessage.builder()
                            .chatId(chatId)
                            .text("Выберите жанр для фильма «" + text + "»:")
                            .replyMarkup(genreKeyboard())
                            .build();
                    try {
                        telegramClient.execute(msg);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    return;
                }

                if (text.equals("/start")) {
                    SendMessage message = SendMessage.builder()
                            .chatId(chatId)
                            .text("Привет! Выбирай действие:")
                            .replyMarkup(mainMenu())
                            .build();

                    long tgID = update.getMessage().getFrom().getId();
                    String username = update.getMessage().getFrom().getUserName();

                    try {
                        telegramClient.execute(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }

                    try {
                        UserData.SaveUser(tgID, username);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private InlineKeyboardMarkup mainMenu() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("Добавить фильм")
                                .callbackData("add_movie")
                                .build()
                ))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("Моя подборка")
                                .callbackData("my_movies")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("Случайный из подборки")
                                .callbackData("random_mine")
                                .build()
                ))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("База фильмов")
                                .callbackData("local_movies")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("Случайный из базы")
                                .callbackData("random_local")
                                .build()
                ))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("Мои отзывы")
                                .callbackData("my_reviews")
                                .build()
                ))
                .build();
    }

    private InlineKeyboardMarkup genreKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("Боевик").callbackData("genre_1").build(),
                        InlineKeyboardButton.builder().text("Комедия").callbackData("genre_2").build()
                ))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("Драма").callbackData("genre_3").build(),
                        InlineKeyboardButton.builder().text("Фантастика").callbackData("genre_4").build()
                ))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("Ужасы").callbackData("genre_5").build(),
                        InlineKeyboardButton.builder().text("Триллер").callbackData("genre_6").build()
                ))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("Мелодрама").callbackData("genre_7").build(),
                        InlineKeyboardButton.builder().text("Детектив").callbackData("genre_8").build()
                ))
                .build();
    }
}