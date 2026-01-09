package com.example.bumil_backend.service;

import com.example.bumil_backend.common.exception.*;
import com.example.bumil_backend.dto.message.ChatMessageDto;
import com.example.bumil_backend.dto.message.MessageListDto;
import com.example.bumil_backend.dto.message.MessageRequest;
import com.example.bumil_backend.entity.ChatMessage;
import com.example.bumil_backend.entity.ChatRoom;
import com.example.bumil_backend.entity.Users;
import com.example.bumil_backend.enums.Role;
import com.example.bumil_backend.repository.ChatMessageRepository;
import com.example.bumil_backend.repository.ChatRoomRepository;
import com.example.bumil_backend.repository.UserRepository;
import com.example.bumil_backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SecurityUtils securityUtils;

    @Transactional
    public void deleteMessage(Long messageId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Users user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("유저를 찾을 수 없습니다."));

        ChatMessage chatMessage = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("메세지를 삭제할 수 없습니다."));

        if(chatMessage.getSender() != user){
            throw new NotAcceptableUserException("메세제 삭제 권한이 없습니다.");
        }

        chatMessage.delete();
    }

    public void sendMessage(MessageRequest request, SimpMessageHeaderAccessor accessor) {
        Principal principal = resolvePrincipal(accessor);
        String username = principal.getName();
        Users user = userRepository.findByEmailAndIsDeletedFalse(username)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        Long roomId = request.getRoomId();

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByIdAndIsDeletedFalse(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 채팅방을 찾을 수 없습니다."));

        ChatMessage chatMessage = ChatMessage.builder()
                .message(request.getMessage())
                .sender(user)
                .chatRoom(chatRoom)
                .build();
        chatMessageRepository.save(chatMessage);

        ChatMessageDto dto = ChatMessageDto.from(chatMessage);

        // 메시지 전송
        messagingTemplate.convertAndSend(
                "/sub/chat/room/" + roomId,
                dto
        );
    }


    private Principal resolvePrincipal(SimpMessageHeaderAccessor accessor) {

        Principal principal = accessor.getUser();
        if (principal != null) {
            return principal;
        }

        Object saved = accessor.getSessionAttributes().get("userPrincipal");
        if (saved instanceof Principal p) {
            return p;
        }

        throw new JwtAuthenticationException("WebSocket 인증 정보가 없습니다.");
    }

    @Transactional(readOnly = true)
    public MessageListDto getMessages(Long chatRoomId, Pageable pageable) {
        Users user = securityUtils.getCurrentUser();

        ChatRoom chatRoom = chatRoomRepository.findByIdAndIsDeletedFalse(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 채팅방을 찾을 수 없습니다."));


        // 작성자 or 관리자
        boolean isAuthor = chatRoom.getAuthor().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isAuthor && !isAdmin) {
            throw new NotAcceptableUserException("해당 채팅방에 대한 권한이 없습니다.");
        }

        List<ChatMessage> messages;

        if (pageable == null) {
            // 오래된 -> 최신으로 정렬
            messages = chatMessageRepository
                    .findByChatRoomAndIsDeletedFalseOrderByCreatedAtAsc(chatRoom);
        } else {
            Pageable sortedPageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.ASC, "createdAt")
            );

            messages = chatMessageRepository
                    .findByChatRoomAndIsDeletedFalse(chatRoom, sortedPageable)
                    .getContent();
        }

        return MessageListDto.from(chatRoom, messages);
    }




}
