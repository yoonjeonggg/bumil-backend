package com.example.bumil_backend.service;

import com.example.bumil_backend.common.exception.BadRequestException;
import com.example.bumil_backend.common.exception.ResourceNotFoundException;
import com.example.bumil_backend.dto.chat.request.ChatCreateRequest;
import com.example.bumil_backend.dto.chat.request.ChatCloseRequest;
import com.example.bumil_backend.dto.chat.request.ChatSettingRequest;
import com.example.bumil_backend.dto.chat.response.ChatCreateResponse;
import com.example.bumil_backend.dto.chat.response.ChatListResponse;
import com.example.bumil_backend.entity.ChatRoom;
import com.example.bumil_backend.entity.DateFilter;
import com.example.bumil_backend.entity.Tag;
import com.example.bumil_backend.entity.Users;
import com.example.bumil_backend.repository.ChatRoomRepository;
import com.example.bumil_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional(readOnly = true)
    public List<ChatListResponse> getPublicChatList(String dateFilter, String tag) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 유저입니다."));

        // 유효성 검증
        validateFilters(dateFilter, tag);

        // 정렬 조건
        Sort sort = "OLDEST".equals(dateFilter) ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();

        // String -> Enum
        Tag searchTag = (tag != null && !tag.isBlank()) ? Tag.valueOf(tag.toUpperCase()) : null;

        List<ChatRoom> chatRooms = chatRoomRepository.findByTag(searchTag, sort);

        return chatRooms.stream()
                .map(chatRoom -> ChatListResponse.builder()
                        .chatRoomId(chatRoom.getId())
                        .title(chatRoom.getTitle())
                        .tag(chatRoom.getTag().name())
                        .author(chatRoom.getAuthor().getName())
                        .createdAt(chatRoom.getCreatedAt())
                        .build()
                )
                .toList();
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

    @Transactional(readOnly = true)
    public List<ChatListResponse> getUserChatList(String dateFilter, String tag) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Users author = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 유저입니다."));

        // 유효성 검증
        validateFilters(dateFilter, tag);

        // 정렬 조건
        Sort sort = "OLDEST".equals(dateFilter) ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();

        // String -> Enum
        Tag searchTag = (tag != null && !tag.isBlank()) ? Tag.valueOf(tag.toUpperCase()) : null;

        List<ChatRoom> chatRooms = chatRoomRepository.findByTagAndAuthor(searchTag, author, sort);

        return chatRooms.stream()
                .map(chatRoom -> ChatListResponse.builder()
                        .chatRoomId(chatRoom.getId())
                        .title(chatRoom.getTitle())
                        .tag(chatRoom.getTag().name())
                        .author(chatRoom.getAuthor().getName())
                        .createdAt(chatRoom.getCreatedAt())
                        .build()
                )
                .toList();
    }

    // 필터 검증
    private void validateFilters(String dateFilter, String tag) {
        if (dateFilter != null && !isValidEnum(DateFilter.class, dateFilter.toUpperCase())) {
            throw new BadRequestException("올바른 날짜 필터를 입력하세요.");
        }

        if (tag != null && !tag.isBlank() && !isValidEnum(Tag.class, tag.toUpperCase())) {
            throw new BadRequestException("올바른 태그를 입력하세요.");
        }
    }


    // Enum 존재 여부 확인
    private <E extends Enum<E>> boolean isValidEnum(Class<E> enumClass, String value) {
        try {
            Enum.valueOf(enumClass, value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void updateChatSetting(ChatSettingRequest request) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        Users user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("유저를 찾을 수 없습니다."));

        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("채팅방을 찾을 수 없습니다."));

        if (!chatRoom.getAuthor().getId().equals(user.getId())) {
            throw new BadRequestException("채팅 설정 변경 권한이 없습니다.");
        }

        if (chatRoom.getTag() == Tag.IN_PROGRESS) {
            throw new BadRequestException("채팅 처리가 완료된 후에만 설정할 수 있습니다.");
        }

        if (request.getIsAnonymous() != null) {
            chatRoom.setAnonymous(request.getIsAnonymous());
        }

        if (request.getIsPublic() != null) {
            chatRoom.setPublic(request.getIsPublic());
        }
    }

}