package com.example.bumil_backend.dto.chat.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatCreateResponse {

    private Long chatId;
    private String title;
    private String tag;
    private LocalDateTime createdAt;
}
