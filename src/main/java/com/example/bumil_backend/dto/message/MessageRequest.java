package com.example.bumil_backend.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class MessageRequest {
    private Long roomId;
    private String message;
}
