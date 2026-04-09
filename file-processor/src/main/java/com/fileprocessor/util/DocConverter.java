package com.fileprocessor.util;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * DOC to DOCX converter utility
 */
public class DocConverter {

    private static final Logger log = LoggerFactory.getLogger(DocConverter.class);

    /**
     * Convert DOC file to DOCX format
     *
     * @param docPath  Source DOC file path
     * @param docxPath Target DOCX file path
     * @return true if conversion successful
     */
    public static boolean convertDocToDocx(String docPath, String docxPath) {
        try (InputStream is = new FileInputStream(docPath);
             HWPFDocument doc = new HWPFDocument(is);
             XWPFDocument docx = new XWPFDocument();
             OutputStream os = new FileOutputStream(docxPath)) {

            log.info("Converting DOC to DOCX: {} -> {}", docPath, docxPath);

            // Process paragraphs
            org.apache.poi.hwpf.usermodel.Range range = doc.getRange();
            for (int i = 0; i < range.numParagraphs(); i++) {
                org.apache.poi.hwpf.usermodel.Paragraph para = range.getParagraph(i);
                XWPFParagraph xwpfPara = docx.createParagraph();

                // Copy paragraph style
                if (para.isInTable()) {
                    xwpfPara.setStyle("TableParagraph");
                }

                for (int j = 0; j < para.numCharacterRuns(); j++) {
                    org.apache.poi.hwpf.usermodel.CharacterRun run = para.getCharacterRun(j);
                    XWPFRun xwpfRun = xwpfPara.createRun();

                    // Copy text
                    xwpfRun.setText(run.text());

                    // Copy formatting
                    xwpfRun.setBold(run.isBold());
                    xwpfRun.setItalic(run.isItalic());
                    xwpfRun.setUnderline(run.getUnderlineCode() > 0 ? org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE : org.apache.poi.xwpf.usermodel.UnderlinePatterns.NONE);

                    // Font size (convert from half-points to points)
                    int fontSize = run.getFontSize();
                    if (fontSize > 0) {
                        xwpfRun.setFontSize(fontSize / 2);
                    }

                    // Font family
                    String fontName = run.getFontName();
                    if (fontName != null) {
                        xwpfRun.setFontFamily(fontName);
                    }

                    // Color
                    if (run.getIco24() != -1) {
                        xwpfRun.setColor(String.format("%06X", run.getIco24()));
                    }
                }
            }

            // Write the DOCX file
            docx.write(os);
            log.info("DOC to DOCX conversion completed: {}", docxPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to convert DOC to DOCX: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Alternative method: Convert DOC to DOCX using HTML bridge
     * This method extracts text and basic formatting through HTML conversion
     */
    public static boolean convertDocToDocxViaHtml(String docPath, String docxPath) {
        try {
            log.info("Converting DOC to DOCX via HTML bridge: {} -> {}", docPath, docxPath);

            // Step 1: Convert DOC to HTML
            String html = convertDocToHtml(docPath);

            // Step 2: Convert HTML to DOCX
            return convertHtmlToDocx(html, docxPath);

        } catch (Exception e) {
            log.error("Failed to convert DOC to DOCX via HTML: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Convert DOC to HTML string
     */
    private static String convertDocToHtml(String docPath) throws Exception {
        try (InputStream is = new FileInputStream(docPath);
             HWPFDocument doc = new HWPFDocument(is);
             StringWriter writer = new StringWriter()) {

            org.w3c.dom.Document htmlDocument = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            org.apache.poi.hwpf.converter.WordToHtmlConverter wordToHtmlConverter = new org.apache.poi.hwpf.converter.WordToHtmlConverter(htmlDocument);
            wordToHtmlConverter.processDocument(doc);

            javax.xml.transform.Transformer transformer = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.METHOD, "html");
            transformer.transform(new javax.xml.transform.dom.DOMSource(htmlDocument), new javax.xml.transform.stream.StreamResult(writer));

            return writer.toString();
        }
    }

    /**
     * Convert HTML string to DOCX
     */
    private static boolean convertHtmlToDocx(String html, String docxPath) {
        try (XWPFDocument docx = new XWPFDocument();
             OutputStream os = new FileOutputStream(docxPath)) {

            // Simple HTML to text extraction and create paragraphs
            org.jsoup.nodes.Document jsoupDoc = org.jsoup.Jsoup.parse(html);
            String text = jsoupDoc.text();

            // Split by common paragraph separators
            String[] paragraphs = text.split("\\n+|\\r\\n+");

            for (String para : paragraphs) {
                if (para.trim().isEmpty()) continue;

                XWPFParagraph xwpfPara = docx.createParagraph();
                XWPFRun run = xwpfPara.createRun();
                run.setText(para.trim());
            }

            docx.write(os);
            return true;

        } catch (Exception e) {
            log.error("Failed to convert HTML to DOCX: {}", e.getMessage(), e);
            return false;
        }
    }
}
