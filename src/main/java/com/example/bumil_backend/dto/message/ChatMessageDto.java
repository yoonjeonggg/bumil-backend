package com.example.bumil_backend.dto.message;

import com.example.bumil_backend.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private String message;
    private boolean isDeleted;
    private Long sender;
    private LocalDateTime createdAt;

    public static ChatMessageDto from(ChatMessage chatMessage) {
        return ChatMessageDto.builder()
                .message(chatMessage.getMessage())
                .isDeleted(chatMessage.isDeleted())
                .sender(chatMessage.getSender().getId())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }

}
