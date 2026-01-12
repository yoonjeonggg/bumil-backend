package com.example.bumil_backend.config;

import com.example.bumil_backend.common.exception.*;
import com.example.bumil_backend.entity.ChatRoom;
import com.example.bumil_backend.entity.Users;
import com.example.bumil_backend.enums.Role;
import com.example.bumil_backend.repository.ChatRoomRepository;
import com.example.bumil_backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRoomStompHandler implements ChannelInterceptor {

    private final ChatRoomRepository chatRoomRepository;
    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;


    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        String destination = accessor.getDestination();

        if (StompCommand.CONNECT.equals(command)) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                throw new JwtAuthenticationException("토큰이 필요합니다.");
            }

            token = token.substring(7); // "Bearer " 제거

            if (!tokenProvider.validateToken(token)) {
                throw new JwtAuthenticationException("유효하지 않은 토큰입니다.");
            }

            String email = tokenProvider.extractEmail(token);
            Users user = userRepository.findByEmailAndIsDeletedFalse(email)
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

            // 세션에 user 저장
            accessor.getSessionAttributes().put("user", user);

            return message;
        }

        if (destination == null) {
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            Long chatRoomId = extractRoomId(destination);
            Users user = (Users) accessor.getSessionAttributes().get("user");
            validateChatAccess(chatRoomId, user);
        }

        if (StompCommand.SEND.equals(command)) {
            String payload = new String((byte[]) message.getPayload());
            Long chatRoomId;
            try {
                chatRoomId = new ObjectMapper().readTree(payload).get("roomId").asLong();
            } catch (Exception e) {
                throw new IllegalArgumentException("SEND 메시지에 roomId가 필요합니다.");
            }

            Users user = (Users) accessor.getSessionAttributes().get("user");
            validateChatAccess(chatRoomId, user);
        }

        return message;
    }

    private void validateChatAccess(Long chatRoomId, Users user) {

        if (user == null) {
            throw new NotLoggedInException("로그인이 필요합니다.");
        }

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("채팅방을 찾을 수 없습니다.")
                );

        if (chatRoom.isDeleted()) {
            throw new ResourceNotFoundException("삭제된 채팅방입니다.");
        }

        boolean isAuthor =
                chatRoom.getAuthor().getId().equals(user.getId());

        boolean isAdmin =
                user.getRole() == Role.ADMIN;

        if (!isAuthor && !isAdmin) {
            throw new NotAcceptableUserException("해당 채팅방에 대한 권한이 없습니다.");
        }
    }


    private Long extractRoomId(String destination) {
        try {
            return Long.parseLong(
                    destination.substring(destination.lastIndexOf("/") + 1)
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 채팅방 경로입니다.");
        }
    }
}
