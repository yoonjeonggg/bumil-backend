package com.example.bumil_backend.dto.chat.response;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Builder
@Getter
public class ChatReactionResponse {
    private Long chatRoomId;
    private Integer likeCnt;
    private Integer dislikeCnt;
}
