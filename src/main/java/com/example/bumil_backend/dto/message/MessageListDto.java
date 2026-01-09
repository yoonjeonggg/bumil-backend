package com.example.bumil_backend.dto.message;

import com.example.bumil_backend.entity.ChatMessage;
import com.example.bumil_backend.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class MessageListDto {
    private Long chatRoomId;
    private String title;
    private List<ChatMessageDto> items;
    private LocalDateTime createdAt;

    public static MessageListDto from(ChatRoom chatRoom, List<ChatMessage> messages) {
        return MessageListDto.builder()
                .chatRoomId(chatRoom.getId())
                .title(chatRoom.getTitle())
                .items(
                        messages.stream()
                                .map(ChatMessageDto::from)
                                .toList()
                )
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }

}
