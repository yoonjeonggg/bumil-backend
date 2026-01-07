package com.example.bumil_backend.controller;

import com.example.bumil_backend.dto.chat.request.ChatCreateRequest;
import com.example.bumil_backend.dto.chat.response.ChatCreateResponse;
import com.example.bumil_backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatCreateResponse createChat(
            @RequestBody @Valid ChatCreateRequest request
    ) {
        return chatService.createChat(request);
    }
}
