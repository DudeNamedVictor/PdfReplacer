package com.example.pdfreplacerbot;

import com.example.pdfreplacerbot.config.BotConfig;
import com.example.pdfreplacerbot.service.Service;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final BotConfig botConfig;
    private final Service service;
    private final TelegramClient client;
    private final Map<Long, String> users = new HashMap<>();

    private final String FILES_DIR = "files/";

    public TelegramBot(BotConfig botConfig, Service service) {
        this.botConfig = botConfig;
        this.service = service;
        this.client = new OkHttpTelegramClient(botConfig.getToken());
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                if (message.getText().equals("/start")) {
                    String response = """
                            Пришлите строку для замены в документе
                            """;

                    sendTextResponse(response, message.getChatId());
                } else {
                    users.put(message.getChatId(), message.getText());

                    String response = """
                            Пришлите pdf документ для обработки
                            """;

                    sendTextResponse(response, message.getChatId());
                }
            }
            if (message.hasDocument()) {
                if (users.get(message.getChatId()) != null) {
                    Document document = message.getDocument();
                    GetFile getFile = new GetFile(document.getFileId());
                    try {
                        File file = client.execute(getFile); //tg file obj
                        java.io.File dFile = client.downloadFile(file);

                        if (!document.getFileName().endsWith(".pdf")) {
                            String response = """
                            Неверный формат файла. Пришлите pdf документ
                            """;
                            sendTextResponse(response, message.getChatId());
                            return;
                        }

                        java.io.File result = service.processPdf(dFile, users.get(message.getChatId()));

                        sendFileResponse(result, message.getChatId(), document.getFileName());
                        FileUtils.deleteDirectory(new java.io.File(FILES_DIR + "/" + message.getChatId()));
                        users.remove(message.getChatId());
                        String response = """
                            Обработка прошла успешно.
                            Пришлите следующую строку для замены в документе
                            """;
                        sendTextResponse(response, message.getChatId());

                    } catch (Exception e) {
                        String response = """
                            Что-то пошло не так.
                            """;

                        sendTextResponse(response + e.getMessage(), message.getChatId());
                        throw new RuntimeException(e);
                    }
                } else {
                    String response = """
                            Пришлите строку для замены в документе и повторите попытку
                            """;
                    sendTextResponse(response, message.getChatId());

                }

            }
        }
    }


    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    private void sendTextResponse(String response, Long chatId) {
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), response);

        try {
            client.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }

    private void sendFileResponse(java.io.File file, Long chatId, String fileName) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        LocalDateTime now = LocalDateTime.now();

        String[] name = fileName.split("\\.");

        InputFile inputFile = new InputFile(file, name[0] + "_" + dtf.format(now) + "." + name[1]);
        SendDocument sendDocument = new SendDocument(String.valueOf(chatId), inputFile);
        try {
            client.execute(sendDocument);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void consume(List<Update> updates) {
        LongPollingSingleThreadUpdateConsumer.super.consume(updates);
    }
}
