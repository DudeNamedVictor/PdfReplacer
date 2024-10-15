package com.example.pdfreplacerbot.service;

import lombok.AllArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public class Service {

    private final static int PAGE_WIDTH = 320;
    private final static int PAGE_HEIGHT = 230;
    private final static int FONT_SIZE = 14;
    private final static int SYMBOLS_ON_LINE = 23;
    private final static int DATAMATRIX_SIZE = 100;
    private final static int DATAMATRIX_X = 15;
    private final static int DATAMATRIX_Y = PAGE_HEIGHT - DATAMATRIX_SIZE - 15;
    private final static int TEXT_X = 170;
    private final static int TEXT_Y = PAGE_HEIGHT - 55;


    public File processPdf(File source, String replacementText) throws IOException {
        File fileResult = File.createTempFile("process", ".pdf");

        File intermediateFile;
        try (PDDocument input = Loader.loadPDF(source)) {
            intermediateFile = createIntermediate(input, replacementText);
        }

        long processingTime = System.currentTimeMillis();
        PDDocument intermediate = Loader.loadPDF(intermediateFile);
        PDFRenderer renderer = new PDFRenderer(intermediate);
        PDDocument result = new PDDocument();
        for (int i = 0; i < intermediate.getNumberOfPages(); i++) {
            BufferedImage pageImage = renderer.renderImage(i, 4, ImageType.GRAY);
            PDPage newPage = new PDPage(new PDRectangle(0, 0, PAGE_WIDTH, PAGE_HEIGHT));
            try (PDPageContentStream stream = new PDPageContentStream(result, newPage)) {
                PDImageXObject imageObject = LosslessFactory.createFromImage(result, pageImage);
                stream.drawImage(imageObject, 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
            }
            pageImage.flush();
            result.addPage(newPage);
        }
        result.save(fileResult);
        result.close();
        intermediateFile.delete();
        System.out.println("Processing time: " + (System.currentTimeMillis() - processingTime) + "ms");

        return fileResult;
    }

    private File createIntermediate(PDDocument input, String replacementText) throws IOException {
        File tmp = Files.createTempFile("intermediate", ".pdf").toFile();
        PDDocument intermediate = new PDDocument();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        PDFont font = PDType0Font.load(intermediate, classLoader.getResourceAsStream("arial.ttf"));
        for (int i = 0; i < input.getNumberOfPages(); i++) {
            PDPage page = input.getPage(i);
            PDXObject xObject;
            try {
                xObject = extractImageOrForm(page);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            if (xObject == null) {
                System.out.println("Image or form not found");
                continue;
            }
            addPage(intermediate, xObject, replacementText, font);
        }
        intermediate.save(tmp);
        intermediate.close();
        return tmp;
    }

    private PDXObject extractImageOrForm(PDPage page) throws IOException {
        PDResources resources = page.getResources();
        if (resources != null) {
            for (COSName objectName : resources.getXObjectNames()) {
                PDXObject xObject = resources.getXObject(objectName);
                if (xObject instanceof PDFormXObject || xObject instanceof PDImageXObject) {
                    return xObject;
                }
            }
        }
        return null;
    }

    private void addPage(PDDocument document, PDXObject xObject, String text, PDFont font) throws IOException {
        PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
        PDPageContentStream stream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
        if (xObject instanceof PDFormXObject formXObject) {
            stream.saveGraphicsState();
            PDRectangle bbox = formXObject.getBBox();
            Matrix matrix = new Matrix(DATAMATRIX_SIZE / bbox.getWidth(),
                    0,
                    0,
                    DATAMATRIX_SIZE / bbox.getHeight(),
                    DATAMATRIX_X,
                    DATAMATRIX_Y);
            stream.transform(matrix);
            stream.drawForm(formXObject);
            stream.restoreGraphicsState();
        } else {
            PDImageXObject imageXObject = (PDImageXObject) xObject;
            imageXObject.setInterpolate(false);
            stream.drawImage(imageXObject, DATAMATRIX_X, DATAMATRIX_Y, DATAMATRIX_SIZE, DATAMATRIX_SIZE);
        }

        stream.beginText();
        stream.setLeading(20);
        stream.newLineAtOffset(TEXT_X, TEXT_Y);

        List<String> lines = splitForLines(text);
        for (String line : lines) {
            stream.setFont(font, FONT_SIZE);
            stream.newLine();
            stream.showText(line);
        }
        stream.endText();
        stream.close();
        document.addPage(page);
    }

    private List<String> splitForLines(String text) {
        List<String> result = new ArrayList<>();
        String[] words = text.split("\\s");
        String line = "";
        for (String word : words) {
            if ((line + word + " ").length() > SYMBOLS_ON_LINE) {
                result.add(line);
                line = word + " ";
            } else {
                line += word + " ";
            }
        }
        result.add(line);
        return result;
    }
}
