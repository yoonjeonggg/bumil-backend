package com.example.bumil_backend.dto.chat.response;

import com.example.bumil_backend.dto.message.ChatMessageDto;
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
public class PublicChatDetailResponse {
    private Long chatRoomId;
    private Integer likeCnt;
    private Integer dislikeCnt;
    private String title;
    private String tag;
    private List<ChatMessageDto> items;
    private LocalDateTime createdAt;

    public static PublicChatDetailResponse from(
            ChatRoom chatRoom,
            List<ChatMessage> messages,
            Integer likeCnt,
            Integer dislikeCnt
    ) {
        return PublicChatDetailResponse.builder()
                .chatRoomId(chatRoom.getId())
                .title(chatRoom.getTitle())
                .likeCnt(likeCnt)
                .dislikeCnt(dislikeCnt)
                .tag(chatRoom.getTag().name())
                .items(
                        ChatMessageDto.from(
                                messages,
                                chatRoom.isAnonymous(),
                                chatRoom.getAuthor().getId()
                        )
                )
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
}

