package ru.artemev.parserforslave.dto;

import lombok.Data;

import java.util.List;

@Data
public class Channel {

    private List<Message> messages;
}
