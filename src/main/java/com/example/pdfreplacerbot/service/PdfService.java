//package com.example.pdfreplacerbot.service;
//
//import com.itextpdf.kernel.colors.ColorConstants;
//import com.itextpdf.kernel.font.PdfFont;
//import com.itextpdf.kernel.font.PdfFontFactory;
//import com.itextpdf.kernel.geom.Rectangle;
//import com.itextpdf.kernel.pdf.PdfDocument;
//import com.itextpdf.kernel.pdf.PdfPage;
//import com.itextpdf.kernel.pdf.PdfReader;
//import com.itextpdf.kernel.pdf.PdfWriter;
//import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
//import com.itextpdf.kernel.pdf.canvas.parser.listener.IPdfTextLocation;
//import com.itextpdf.kernel.utils.PageRange;
//import com.itextpdf.kernel.utils.PdfMerger;
//import com.itextpdf.kernel.utils.PdfSplitter;
//import com.itextpdf.layout.Canvas;
//import com.itextpdf.layout.element.Paragraph;
//import com.itextpdf.pdfcleanup.PdfCleaner;
//import com.itextpdf.pdfcleanup.autosweep.CompositeCleanupStrategy;
//import com.itextpdf.pdfcleanup.autosweep.RegexBasedCleanupStrategy;
//import lombok.AllArgsConstructor;
//import org.springframework.stereotype.Component;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.Iterator;
//import java.util.List;
//
//@Component
//@AllArgsConstructor
//public class PdfService {
//    public File processPdf(File source, String replacementText) throws IOException {
//
//        String parentPath = source.getParent();
//
//        String tmp1Path = parentPath + "/tmp1/";
//        String tmp2Path = parentPath + "/tmp2/";
//
//        File tmp1Dir = new File(tmp1Path);
//        File tmp2Dir = new File(tmp2Path);
//
//        tmp1Dir.mkdirs();
//        tmp2Dir.mkdirs();
//
//        String destPath = tmp1Path + "%s.pdf";
//        String resultPath = source.getParent() + "/result.pdf";
//
//        File resultFile = new File(resultPath);
//        File destFile = new File(destPath);
//
//        split(destPath, source);
//
//        PdfDocument result = new PdfDocument(new PdfWriter(resultFile));
//
//        File[] listOfFiles = tmp1Dir.listFiles();
//        Arrays.sort(listOfFiles, new Comparator<File>() {
//            public int compare(File f1, File f2) {
//                try {
//                    int i1 = Integer.parseInt(f1.getName().replaceAll("\\.pdf", ""));
//                    int i2 = Integer.parseInt(f2.getName().replaceAll("\\.pdf", ""));
//                    return i1 - i2;
//                } catch(NumberFormatException e) {
//                    throw new AssertionError(e);
//                }
//            }
//        });
//
//        for (File f : listOfFiles) {
//            replaceText(f, replacementText);
//        }
//
//        listOfFiles = tmp2Dir.listFiles();
//        Arrays.sort(listOfFiles, new Comparator<File>() {
//            public int compare(File f1, File f2) {
//                try {
//                    int i1 = Integer.parseInt(f1.getName().replaceAll("\\.pdf", ""));
//                    int i2 = Integer.parseInt(f2.getName().replaceAll("\\.pdf", ""));
//                    return i1 - i2;
//                } catch(NumberFormatException e) {
//                    throw new AssertionError(e);
//                }
//            }
//        });
//
//        for (File f : listOfFiles) {
//            merge(result, f);
//        }
//
//        result.close();
//
//        return resultFile;
//
//    }
//
//    private void split(String dest, File source) throws IOException {
//        PdfDocument pdfDoc = new PdfDocument(new PdfReader(source));
//
//        List<PdfDocument> splitDocuments = new PdfSplitter(pdfDoc) {
//            int partNumber = 1;
//
//            @Override
//            protected PdfWriter getNextPdfWriter(PageRange documentPageRange) {
//                try {
//                    return new PdfWriter(String.format(dest, partNumber++));
//                } catch (FileNotFoundException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }.splitBySize(200000);
//
//        for (PdfDocument doc : splitDocuments) {
//            doc.close();
//        }
//
//        pdfDoc.close();
//    }
//
//
//    public static void merge(PdfDocument target, File file) throws IOException {
//        PdfDocument pdfDocument2 = new PdfDocument(new PdfReader(file));
//
//        PdfMerger merger = new PdfMerger(target);
//        merger.merge(pdfDocument2, 1, pdfDocument2.getNumberOfPages());
//
//        pdfDocument2.close();
//    }
//
//    public static void replaceText(File file, String replacementText) throws IOException {
//        PdfReader reader = new PdfReader(file);
//        File file2 = new File(file.getAbsolutePath().replaceAll("tmp1", "tmp2"));
//        file2.getParentFile().mkdirs();
//        PdfWriter writer = new PdfWriter(file2);
//        PdfDocument pdfDocument = new PdfDocument(reader, writer);
//
//        String FONT = "assets/fonts/arial.ttf";
//        PdfFont font = PdfFontFactory.createFont(FONT, "Cp1251");
//
//        CompositeCleanupStrategy strategy = new CompositeCleanupStrategy();
//        strategy.add(new RegexBasedCleanupStrategy(".*").setRedactionColor(ColorConstants.WHITE));
//        PdfCleaner.autoSweepCleanUp(pdfDocument, strategy);
//
//        Iterator<IPdfTextLocation> iterator =  strategy.getResultantLocations().iterator();
//        IPdfTextLocation location = iterator.next();
//        while (true) {
//            if (iterator.hasNext()) {
//                location = iterator.next();
//            } else {
//                PdfPage page2 = pdfDocument.getPage(location.getPageNumber() + 1);
//                PdfCanvas pdfCanvas = new PdfCanvas(page2.newContentStreamAfter(), page2.getResources(), page2.getDocument());
//                Rectangle rectangle = location.getRectangle();
//                rectangle.setBbox(rectangle.getLeft(), rectangle.getBottom() - rectangle.getHeight() * 6 , rectangle.getRight(), rectangle.getTop());
//                Canvas canvas = new Canvas(pdfCanvas, rectangle);
//
//                canvas.add(new Paragraph(replacementText).setFontSize(4).setMarginTop(0f).setFont(font)); //text here
//                canvas.close();
//                break;
//            }
//        }
//        pdfDocument.close();
//
//    }
//}
