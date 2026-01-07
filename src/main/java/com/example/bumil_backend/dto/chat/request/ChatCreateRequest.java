package com.example.bumil_backend.dto.chat.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ChatCreateRequest {

    @NotBlank
    private String title;
}
