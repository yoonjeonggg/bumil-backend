package com.example.bumil_backend.controller;

import com.example.bumil_backend.common.ApiResponse;
import com.example.bumil_backend.dto.chat.request.ChatCloseRequest;
import com.example.bumil_backend.dto.chat.request.ChatCreateRequest;
import com.example.bumil_backend.dto.chat.request.ChatSettingRequest;
import com.example.bumil_backend.dto.chat.response.ChatCreateResponse;
import com.example.bumil_backend.dto.chat.response.PublicChatListResponse;
import com.example.bumil_backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;


    @PostMapping
    public ResponseEntity<ApiResponse<ChatCreateResponse>> createChat(
            @RequestBody @Valid ChatCreateRequest request
    ) {
        return ApiResponse.ok(
                chatService.createChat(request),
                "채팅방이 생성되었습니다."
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PublicChatListResponse>>> getPublicChatListResponse(
            @RequestParam(value = "datefilter", required = false) String dateFilter,
            @RequestParam(value = "tag", required = false) String tag
    ){
        return ApiResponse.ok(chatService.getPublicChatList(dateFilter, tag), "공개 채팅 목록 조회에 성공하였습니다.");
    }


    @PatchMapping("/close")
    public ResponseEntity<ApiResponse<Void>> closeChat(
            @RequestBody ChatCloseRequest request
    ) {
        chatService.closeChat(request);
        return ApiResponse.ok(null, "채팅 상태가 변경되었습니다.");
    }

    @PatchMapping("/setting")
    public ResponseEntity<ApiResponse<Void>> updateChatSetting(
            @RequestBody @Valid ChatSettingRequest request
    ) {
        chatService.updateChatSetting(request);
        return ApiResponse.ok(null, "채팅 설정이 변경되었습니다.");
    }

}
