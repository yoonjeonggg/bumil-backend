package com.example.bumil_backend.service;

import com.example.bumil_backend.common.exception.BadRequestException;
import com.example.bumil_backend.common.exception.ResourceNotFoundException;
import com.example.bumil_backend.dto.chat.request.ChatCreateRequest;
import com.example.bumil_backend.dto.chat.request.ChatCloseRequest;
import com.example.bumil_backend.dto.chat.response.ChatCreateResponse;
import com.example.bumil_backend.entity.ChatRoom;
import com.example.bumil_backend.entity.Tag;
import com.example.bumil_backend.entity.Users;
import com.example.bumil_backend.repository.ChatRoomRepository;
import com.example.bumil_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    // 채팅방 생성
    public ChatCreateResponse createChat(ChatCreateRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName(); // JWT sub

        Users user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("유저를 찾을 수 없습니다."));

        ChatRoom chatRoom = ChatRoom.builder()
                .title(request.getTitle())
                .tag(Tag.IN_PROGRESS)
                .author(user)
                .build();

        ChatRoom saved = chatRoomRepository.save(chatRoom);

        return ChatCreateResponse.builder()
                .chatId(saved.getId())
                .title(saved.getTitle())
                .tag(saved.getTag().name())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    // 채팅방 상태 병경(준비중-> 채택, 반려, 종료)
    public void closeChat(ChatCloseRequest request) {

        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("채팅방을 찾을 수 없습니다."));

        // 이미 처리된 채팅이면 예외
        if (chatRoom.getTag() != Tag.IN_PROGRESS) {
            throw new BadRequestException("이미 처리된 채팅방입니다.");
        }

        chatRoom.setTag(request.getTag());
    }
}
