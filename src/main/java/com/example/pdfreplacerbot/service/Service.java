package com.example.pdfreplacerbot.service;

import lombok.AllArgsConstructor;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
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
        File fileResult = new File("files/result.pdf");

        PDDocument input = PDDocument.load(source);
        PDDocument result = new PDDocument();
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
            addPage(result, xObject, replacementText);
        }
        result.save(fileResult);
        input.close();
        result.close();

        return fileResult;
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

    private void addPage(PDDocument document, PDXObject xObject, String text) throws IOException {
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
            stream.drawImage(imageXObject, DATAMATRIX_X, DATAMATRIX_Y, DATAMATRIX_SIZE, DATAMATRIX_SIZE);
        }

        stream.beginText();
        stream.setLeading(20);
        stream.newLineAtOffset(TEXT_X, TEXT_Y);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        PDFont font = PDType0Font.load(document, classLoader.getResourceAsStream("arial.ttf"));

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
