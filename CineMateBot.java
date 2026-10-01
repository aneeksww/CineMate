package org.example;

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
import java.util.List;



public class CineMateBot implements LongPollingUpdateConsumer {

    private final TelegramClient telegramClient;
    private final UserData UserData;

    public CineMateBot(String token) {
        telegramClient = new OkHttpTelegramClient(token);
        UserData = new UserData();
    }

    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) {

            if (update.hasCallbackQuery()) {

                String data = update.getCallbackQuery().getData();
                long ChatID = update.getCallbackQuery().getMessage().getChatId();

                if (data.equals("main_menu")) {

                    EditMessageText message = EditMessageText.builder()
                            .chatId(ChatID)
                            .messageId(update.getCallbackQuery().getMessage().getMessageId())
                            .text("ляляля я семен лобанов выбирай")
                            .replyMarkup(mainMenu())
                            .build();

                    try {
                        telegramClient.execute(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }

                    return;
                }

                String answer = switch(data) {
                    case "add_movie" ->
                        "типа идет добавление фильма";
                    case "my_movies" ->
                        "типа подборка введенных фильмов";
                    case "random_mine" ->
                        "типа жесткий рандом из подборки";
                    case "local_movies" ->
                        "типа встроенная база";
                    case "random_local" ->
                        "типа жесткий рандом из базы";
                    case "my_reviews" ->
                        "типа наши отзывы";
                    default ->
                        "куда ты нажал";
                };

                InlineKeyboardMarkup backKeyboard = InlineKeyboardMarkup.builder()
                        .keyboardRow(new InlineKeyboardRow(
                                InlineKeyboardButton.builder()
                                        .text("⬅ Главное меню")
                                        .callbackData("main_menu")
                                        .build()
                        ))
                        .build();

                EditMessageText message = EditMessageText.builder()
                        .chatId(ChatID)
                        .messageId(update.getCallbackQuery().getMessage().getMessageId())
                        .text(answer)
                        .replyMarkup(backKeyboard)
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



                if (text.equals("/start")) {


                    SendMessage message = SendMessage.builder()
                            .chatId(chatId)
                            .text("ляляля я семен лобанов выбирай")
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
}