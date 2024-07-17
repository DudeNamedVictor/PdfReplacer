package com.example.pdfreplacerbot;

import com.example.pdfreplacerbot.service.Service;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@SpringBootApplication
public class PdfReplacerBotApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(PdfReplacerBotApplication.class, args);
        (new File("files")).mkdir();
    }
}
