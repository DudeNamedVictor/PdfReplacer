package com.example.pdfreplacerbot;

import com.example.pdfreplacerbot.config.BotConfig;
import com.example.pdfreplacerbot.service.Service;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class TelegramBot implements SpringLongPollingBot, LongPollingUpdateConsumer {

    private final BotConfig botConfig;
    private final Service service;
    private final TelegramClient client;
    private final Map<Long, Document> users = new HashMap<>();

    private final Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public TelegramBot(BotConfig botConfig, Service service) {
        this.botConfig = botConfig;
        this.service = service;
        this.client = new OkHttpTelegramClient(botConfig.getToken());
    }

    public void consume(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            if (message.hasDocument()) {
                Document document = message.getDocument();
                if (!document.getFileName().endsWith(".pdf")) {
                    String response = """
                            Неверный формат файла. Пришлите pdf документ.
                            """;
                    sendTextResponse(response, chatId);
                    return;
                }
                users.put(message.getChatId(), document);

                String response = """
                        Напишите текст, который необходимо вставить в документ.
                        """;
                sendTextResponse(response, chatId);
            }
            if (message.hasText()) {
                if (message.getText().equals("/start")) {
                    String response = """
                            Загрузите исходный файл Честного Знака, в формате PDF.
                            """;

                    sendTextResponse(response, message.getChatId());
                } else {
                    String replacementText = message.getText();
                    Document document = users.get(chatId);
                    if (document == null) {
                        String response = """
                                Сначала загрузите файл, затем пришлите текст.
                                """;
                        sendTextResponse(response, message.getChatId());
                        return;
                    }
                    try {
                        GetFile getFile = new GetFile(document.getFileId());
                        File file = client.execute(getFile); //tg file obj
                        java.io.File dFile = client.downloadFile(file);

                        java.io.File result = service.processPdf(dFile, replacementText);
                        sendFileResponse(result, chatId, document.getFileName());
                        users.remove(chatId);
                        String response = """
                            Обработка прошла успешно. (пока что бот генерирует файл только в формате 58*40, если бот будет пользоваться популярностью, добавим еще размеры)
                            
                            Важно! Проверяйте несколько QR-кодов из файла с помощью приложения Честный Знак.
                            Честный знак может внести изменения в алгоритмы работы кода, вам нужно убедиться, что файл, сгенерирован корректно. Если вы проверите несколько QR-кодов и они работают, то все остальные коды в этом файле будут работать правильно.
                            """;
                        sendTextResponse(response, chatId);

                        dFile.delete();
                        result.delete();
                        users.remove(chatId);
                    } catch (Exception e) {
                        String response = """
                            Что-то пошло не так.
                            """;

                        sendTextResponse(response + e.getMessage(), message.getChatId());
                        throw new RuntimeException(e);
                    }
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
        updates.forEach((update) -> executor.execute(() -> consume(update)));
    }
}
