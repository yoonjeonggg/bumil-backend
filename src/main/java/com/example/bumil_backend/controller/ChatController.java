package com.example.bumil_backend.controller;

import com.example.bumil_backend.common.ApiResponse;
import com.example.bumil_backend.dto.chat.response.ChatReactionResponse;
import com.example.bumil_backend.dto.chat.request.ChatCloseRequest;
import com.example.bumil_backend.dto.chat.request.ChatCreateRequest;
import com.example.bumil_backend.dto.chat.request.ChatReactionRequest;
import com.example.bumil_backend.dto.chat.request.ChatSettingRequest;
import com.example.bumil_backend.dto.chat.response.ChatCreateResponse;
import com.example.bumil_backend.dto.chat.response.ChatListResponse;
import com.example.bumil_backend.dto.chat.response.PublicChatListResponse;
import com.example.bumil_backend.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Create a Chat", description = "채팅방 생성 API")
    public ResponseEntity<ApiResponse<ChatCreateResponse>> createChat(
            @RequestBody @Valid ChatCreateRequest request
    ) {
        return ApiResponse.ok(
                chatService.createChat(request),
                "채팅방이 생성되었습니다."
        );
    }

    @GetMapping
    @Operation(summary = "Get public chats by filtering", description = "공개된 채팅 목록 조회 API")
    public ResponseEntity<ApiResponse<List<ChatListResponse>>> getPublicChatListResponse(
            @RequestParam(value = "datefilter", required = false) String dateFilter,
            @RequestParam(value = "tag", required = false) String tag
    ){
        return ApiResponse.ok(chatService.getPublicChatList(dateFilter, tag), "공개 채팅 목록 조회에 성공하였습니다.");
    }

    @GetMapping("/me")
    @Operation(summary = "Get user's chats by filtering", description = "유저의 채팅 목록 조회 API")
    public ResponseEntity<ApiResponse<List<ChatListResponse>>> getUserChatListResponse(
            @RequestParam(value = "datefilter", required = false) String dateFilter,
            @RequestParam(value = "tag", required = false) String tag
    ){
        return ApiResponse.ok(chatService.getUserChatList(dateFilter, tag), "유저 채팅 목록 조회에 성공하였습니다.");
    }

    @PatchMapping("/close")
    @Operation(summary = "Close a chat", description = "채팅방 마감 API")
    public ResponseEntity<ApiResponse<Void>> closeChat(
            @RequestBody ChatCloseRequest request
    ) {
        chatService.closeChat(request);
        return ApiResponse.ok(null, "채팅 상태가 변경되었습니다.");
    }

    @PatchMapping("/setting")
    @Operation(summary = "Update chat settings", description = "채팅방 설정 변경 API")
    public ResponseEntity<ApiResponse<Void>> updateChatSetting(
            @RequestBody @Valid ChatSettingRequest request
    ) {
        chatService.updateChatSetting(request);
        return ApiResponse.ok(null, "채팅 설정이 변경되었습니다.");
    }

    @PatchMapping("/{chatRoomId}")
    @Operation(summary = "채팅 삭제", description = "관리자와 작성자만 삭제할 수 있습니다.")
    public ResponseEntity<ApiResponse<Void>> deleteChat(@PathVariable Long chatRoomId) {
        chatService.deleteChat(chatRoomId);
        return ApiResponse.ok(null, "채팅이 삭제되었습니다.");
    }


    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PublicChatListResponse>>> searchPublicChats(
            @RequestParam("query") String query
    ) {
        return ApiResponse.ok(
                chatService.searchPublicChats(query),
                "공개 채팅방 검색에 성공했습니다."
        );
    }

    @PatchMapping("/reaction")
    @Operation(summary = "리액션 설정", description = "공개된 채팅방에 리액션 설정 API")
    public ResponseEntity<ApiResponse<ChatReactionResponse>> reaction(@RequestBody @Valid ChatReactionRequest request){
        ChatReactionResponse response = chatService.reaction(request);
        return ApiResponse.ok(response, "공감 설정이 완료되었습니다.");
    }

}
