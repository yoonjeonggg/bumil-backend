package com.example.bumil_backend.dto.chat.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class PublicChatListResponse {
    private String title;
    private String tag;
    private LocalDateTime createdAt;
}
