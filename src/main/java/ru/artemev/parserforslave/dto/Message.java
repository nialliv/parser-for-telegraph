package ru.artemev.parserforslave.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Message {

    private Integer id;

    @JsonProperty("text_entities")
    private List<TextEntity> textEntities;

}
