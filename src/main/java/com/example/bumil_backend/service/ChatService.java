package com.example.bumil_backend.service;

import com.example.bumil_backend.common.exception.BadRequestException;
import com.example.bumil_backend.common.exception.ResourceNotFoundException;
import com.example.bumil_backend.dto.chat.request.ChatCreateRequest;
import com.example.bumil_backend.dto.chat.response.ChatCreateResponse;
import com.example.bumil_backend.dto.chat.response.PublicChatListResponse;
import com.example.bumil_backend.entity.ChatRoom;
import com.example.bumil_backend.entity.DateFilter;
import com.example.bumil_backend.entity.Tag;
import com.example.bumil_backend.entity.Users;
import com.example.bumil_backend.repository.ChatRoomRepository;
import com.example.bumil_backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    public ChatCreateResponse createChat(ChatCreateRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName(); // JWT sub

        Users user = userRepository.findByEmail(email)
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

    @Transactional(readOnly = true)
    public List<PublicChatListResponse> getPublicChatList(String dateFilter, String tag) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 유저입니다."));

        if(!dateFilter.matches(Arrays.toString(DateFilter.values())) && !tag.matches(Arrays.toString(Tag.values()))){
            throw new BadRequestException("올바른 필터링을 입력하세요.");
        }

        Sort sort = dateFilter.equals("RECENT") ? Sort.by("createdAt").descending() : Sort.by("createdAt").ascending();
        List<ChatRoom> chatRooms = chatRoomRepository.findByTag(tag, sort);

        if (chatRooms.isEmpty()) {
            return List.of();   // null 대신 빈 리스트 권장
        }

        return chatRooms.stream()
                .map(chatRoom -> PublicChatListResponse.builder()
                        .title(chatRoom.getTitle())
                        .tag(chatRoom.getTag().name())
                        .createdAt(chatRoom.getCreatedAt())
                        .build()
                )
                .toList();
    }

}
