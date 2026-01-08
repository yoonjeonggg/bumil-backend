package com.example.bumil_backend.dto.chat.request;

import lombok.Getter;

@Getter
public class ChatSettingRequest {
    private Long chatRoomId;
    private Boolean isAnonymous;
    private Boolean isPublic;
}
