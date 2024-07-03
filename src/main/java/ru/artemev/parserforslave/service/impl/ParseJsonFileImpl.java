package ru.artemev.parserforslave.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import ru.artemev.parserforslave.dto.Channel;
import ru.artemev.parserforslave.dto.Message;
import ru.artemev.parserforslave.dto.TextEntity;
import ru.artemev.parserforslave.service.DocxCreator;
import ru.artemev.parserforslave.service.ParseJsonFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParseJsonFileImpl implements ParseJsonFile {

    private static final String TEXT_LINK = "text_link";
    private static final Set<Integer> CHAPTERS_SAVED = new HashSet<>();
    private static final Set<String> NOT_DOWNLOADED_TEXTS = new HashSet<>();


    private final ObjectMapper objectMapper;
    private final DocxCreator docxCreator;
    private final List<String> urlsForDownload = List.of();
    @Value("${path-to-json-file}")
    private String pathToJsonFile;

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void initProcessSever() {
        try {
            Channel channel = objectMapper.readValue(Paths.get(pathToJsonFile).toFile(), Channel.class);
            channel.getMessages()
                    .forEach(this::saveProcess);
            log.info("Finish");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void anotherAttempt() {
        for (String url : urlsForDownload) {
            try {
                docxCreator.downloadFileFromUrl(url);
            } catch (Exception e) {
                log.error("Another attempt failed", e);
                NOT_DOWNLOADED_TEXTS.add(url);
            }
        }
        log.warn("THIS IS NOT SAVED - [{}]", NOT_DOWNLOADED_TEXTS);
    }

    private void saveProcess(Message message) {
        List<TextEntity> textEntities = message.getTextEntities();
        if (textEntities.isEmpty()) {
            return;
        }
        for (TextEntity textEntity : textEntities) {
            if (TEXT_LINK.equals(textEntity.getType()) && checkChapterNum(textEntity.getText())) {
                log.info("Found chapter - [{}]", textEntity.getText());
                try {
                    docxCreator.downloadFileFromUrl(textEntity.getHref());
                } catch (Exception e) {
                    log.error("Failed to download file from url", e);
                    NOT_DOWNLOADED_TEXTS.add(textEntity.getText());
                }
            }
        }
        log.warn("THIS IS NOT SAVED - [{}]", NOT_DOWNLOADED_TEXTS);
    }

    private boolean checkChapterNum(String text) {
        String[] s = text.split(" ");
        if (s.length != 2) {
            log.warn("This is not chapter number");
            return false;
        }
        try {
            Integer chapterNum = Integer.parseInt(s[1]);
            if (CHAPTERS_SAVED.contains(chapterNum)) {
                log.warn("Chapter number already saved");
                return false;
            }
            CHAPTERS_SAVED.add(chapterNum);
            return true;
        } catch (NumberFormatException e) {
            log.info("Chapter number is not valid", e);
            return false;
        }
    }
}
