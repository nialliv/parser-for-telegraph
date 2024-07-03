package ru.artemev.parserforslave.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.artemev.parserforslave.service.DocxCreator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DocxCreatorImpl implements DocxCreator {

    @Value("${path-to-dir:}")
    private String pathToDir;

    @Override
    public void downloadFileFromUrl(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            Element article = doc.getElementsByTag("article").get(0);
            String chapterName = article.getElementsByTag("h1").get(0).text();
            List<String> paragraphs = new ArrayList<>();
            article.getElementsByTag("p")
                    .stream()
                    .map(Element::text)
                    .forEach(paragraphs::add);
            log.info("Got data from telegraph. Chapter: {}", chapterName);
            saveDataInDocument(chapterName, paragraphs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveDataInDocument(String chapterName, List<String> paragraphs) {
        log.info("Started to save data in document");
        try {
            WordprocessingMLPackage wordPackage = WordprocessingMLPackage.createPackage();
            MainDocumentPart mainDocumentPart = wordPackage.getMainDocumentPart();
            mainDocumentPart.addStyledParagraphOfText("Title", chapterName);
            mainDocumentPart.addParagraphOfText("");
            paragraphs
                    .forEach(mainDocumentPart::addParagraphOfText);
            wordPackage.save(new File(pathToDir + chapterName.substring(0, chapterName.lastIndexOf(":")) + ".docx"));
        } catch (Docx4JException e) {
            throw new RuntimeException(e);
        }
        log.info("Finished to save data in document");
    }
}
