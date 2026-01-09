package com.example.bumil_backend.controller;

import com.example.bumil_backend.common.ApiResponse;
import com.example.bumil_backend.dto.message.MessageListDto;
import com.example.bumil_backend.dto.message.MessageRequest;
import com.example.bumil_backend.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/message")
public class MessageController {
    private final MessageService messageService;

    @PatchMapping("/{messageId}")
    @Operation(summary = "Delete Message", description = "메세지 삭제 API")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable Long messageId
    ){
        messageService.deleteMessage(messageId);
        return ApiResponse.ok("삭제된 메세지입니다.");
    }


    @PostMapping("/send")
    @Operation(
            summary = "채팅 메시지 전송 (WebSocket)",
            description = """
    ⚠️ 본 API는 Swagger 문서 표시용입니다.

    실제 메시지 전송은 WebSocket(STOMP)을 사용합니다.

    ▶ SEND
    /pub/chat/send

    ▶ SUBSCRIBE
    /sub/chat/room/{roomId}
    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "요청 성공"
            ), @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "해당 채팅방을 찾을 수 없습니다.",
            content = @Content
    ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "해당 사용자를 찾을 수 없습니다.",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "로그인이 필요합니다.",
                    content = @Content
            )
    })
    public String chatSendMessage() {
        return "Swagger 표시용 엔드포인트입니다.";
    }

    @MessageMapping("/chat/send")
    @Operation(summary = "채팅방 메시지 전송", description = "채팅방 메시지 전송 시 사용하는 API 입니다.")
    public void sendMessage(@Payload MessageRequest request, SimpMessageHeaderAccessor accessor) {
        messageService.sendMessage(request, accessor);
    }

    @GetMapping("/{chatRoomId}")
    @Operation(summary = "Get messages", description = "채팅방 메시지 목록 조회 API")
    public ResponseEntity<ApiResponse<MessageListDto>> getMessages
            (@PathVariable Long chatRoomId,
             @RequestParam(required = false) Pageable pageable
             ) {
        MessageListDto result = messageService.getMessages(chatRoomId, pageable);
        return ApiResponse.ok(result, "채팅방 메시지 목록이 조회되었습니다.");

    }
}
