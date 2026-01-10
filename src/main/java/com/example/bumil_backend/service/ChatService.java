package com.example.bumil_backend.service;

import com.example.bumil_backend.common.exception.BadRequestException;
import com.example.bumil_backend.common.exception.NotAcceptableUserException;
import com.example.bumil_backend.common.exception.ResourceNotFoundException;
import com.example.bumil_backend.dto.chat.response.ChatReactionResponse;
import com.example.bumil_backend.dto.chat.request.ChatCreateRequest;
import com.example.bumil_backend.dto.chat.request.ChatCloseRequest;
import com.example.bumil_backend.dto.chat.request.ChatReactionRequest;
import com.example.bumil_backend.dto.chat.request.ChatSettingRequest;
import com.example.bumil_backend.dto.chat.response.ChatCreateResponse;
import com.example.bumil_backend.dto.chat.response.ChatListResponse;
import com.example.bumil_backend.dto.chat.response.PublicChatListResponse;
import com.example.bumil_backend.entity.ChatRoom;
import com.example.bumil_backend.entity.ChatRoomReaction;
import com.example.bumil_backend.entity.DateFilter;
import com.example.bumil_backend.entity.Users;
import com.example.bumil_backend.enums.ChatTags;
import com.example.bumil_backend.enums.ReactionType;
import com.example.bumil_backend.enums.Role;
import com.example.bumil_backend.enums.Tag;
import com.example.bumil_backend.repository.ChatRoomReactionRepository;
import com.example.bumil_backend.repository.ChatRoomRepository;
import com.example.bumil_backend.repository.UserRepository;
import com.example.bumil_backend.security.SecurityUtils;
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
    private final SecurityUtils securityUtils;
    private final ChatRoomReactionRepository chatRoomReactionRepository;

    // 채팅방 생성
    public ChatCreateResponse createChat(ChatCreateRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName(); // JWT sub

        Users user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("유저를 찾을 수 없습니다."));

        ChatRoom chatRoom = ChatRoom.builder()
                .title(request.getTitle())
                .tag(ChatTags.IN_PROGRESS)
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

        List<ChatRoom> chatRooms = chatRoomRepository.findAllByTagAndIsDeletedFalse(searchTag, sort);

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
        if (chatRoom.getTag() != ChatTags.IN_PROGRESS) {
            throw new BadRequestException("이미 처리된 채팅방입니다.");
        }

        chatRoom.changeTag(request.getTag());
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

        List<ChatRoom> chatRooms = chatRoomRepository.findByTagAndAuthorAndIsDeletedFalse(searchTag, author, sort);
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

        if (chatRoom.getTag() == ChatTags.IN_PROGRESS) {
            throw new BadRequestException("채팅 처리가 완료된 후에만 설정할 수 있습니다.");
        }

        if (request.getIsAnonymous() != null) {
            chatRoom.changeAnonymous(request.getIsAnonymous());
        }

        if (request.getIsPublic() != null) {
            chatRoom.setPublic(request.getIsPublic());
        }
    }

    // 채팅 삭제
    @Transactional
    public void deleteChat(Long chatRoomId) {
        Users user = securityUtils.getCurrentUser();

        ChatRoom chatRoom = chatRoomRepository.findByIdAndIsDeletedFalse(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("이미 삭제된 채팅방 입니다."));
        // 관리자 or 작성자
        boolean isAuthor =
                chatRoom.getAuthor().getId().equals(user.getId());

        boolean isAdmin =
                user.getRole() == Role.ADMIN;

        if (!isAuthor && !isAdmin) {
            throw new NotAcceptableUserException("해당 채팅방에 대한 권한이 없습니다.");
        }
        chatRoom.delete();
    }

    @Transactional(readOnly = true)
    public List<PublicChatListResponse> searchPublicChats(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new BadRequestException("검색어(query)는 필수입니다.");
        }

        List<ChatRoom> chatRooms = chatRoomRepository.searchPublicChatsByTitle(query.trim());

        return chatRooms.stream()
                .map(chatRoom -> PublicChatListResponse.builder()
                        .title(chatRoom.getTitle())
                        .tag(chatRoom.getTag().name())
                        .createdAt(chatRoom.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public ChatReactionResponse reaction(ChatReactionRequest request) {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        Users user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("유저를 찾을 수 없습니다."));

        ChatRoom chatRoom = chatRoomRepository.findByIdAndIsDeletedFalseAndIsPublicTrue(request.getChatRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("해당 채팅방을 찾을 수 없습니다."));

        ChatRoomReaction existingReaction =
                chatRoomReactionRepository.findByUserAndChatRoom(user, chatRoom);

        //유저가 리액션을 누르지 않은 경우
        if(existingReaction == null){
            ChatRoomReaction savedReaction = ChatRoomReaction.builder()
                    .chatRoom(chatRoom)
                    .user(user)
                    .reactionType(request.getReactionType())
                    .build();

            chatRoomReactionRepository.save(savedReaction);
            user.addReaction(savedReaction);
            chatRoom.addReaction(savedReaction);
        }

        //유저가 이미 리액션을 누른 경우
        // 기존 리액션이 있는 경우
        else if (existingReaction.getReactionType() == request.getReactionType()) {
            // 같은 리액션 -> 해제
            chatRoomReactionRepository.delete(existingReaction);
            user.removeReaction(existingReaction);
            chatRoom.removeReaction(existingReaction);
        } else {
            // 다른 리액션 -> 수정
            existingReaction.changeReactionType(request.getReactionType());
        }

        return countingReactions(chatRoom);
    }

    private ChatReactionResponse countingReactions(ChatRoom chatRoom) {

        Integer likeCnt = (int) chatRoom.getReactions().stream()
                .filter(r -> r.getReactionType() == ReactionType.LIKE)
                .count();

        Integer dislikeCnt = (int) chatRoom.getReactions().stream()
                .filter(r -> r.getReactionType() == ReactionType.DISLIKE)
                .count();

        return ChatReactionResponse.builder()
                .chatRoomId(chatRoom.getId())
                .likeCnt(likeCnt)
                .dislikeCnt(dislikeCnt)
                .build();
    }
}