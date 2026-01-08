package com.example.bumil_backend.dto.chat.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class ChatListResponse {
    private Long chatRoomId;
    private String title;
    private String tag;
    private String author;
    private LocalDateTime createdAt;
}
