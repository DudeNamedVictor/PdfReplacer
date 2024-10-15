package com.example.pdfreplacerbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;

@SpringBootApplication
public class PdfReplacerBotApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(PdfReplacerBotApplication.class, args);
        (new File("files")).mkdir();
    }
}
